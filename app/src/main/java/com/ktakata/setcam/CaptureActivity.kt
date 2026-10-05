package com.ktakata.setcam

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.effects.OverlayEffect
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.preference.PreferenceManager
import com.ktakata.setcam.settings.Resolution
import com.ktakata.setcam.settings.SetcamSettings
import com.ktakata.setcam.stamp.StampRenderer
import com.ktakata.setcam.stamp.StampStyle
import com.ktakata.setcam.stamp.UprightTransform
import java.time.LocalDateTime

/** 起動したら（必要ならカウントダウンの後で）2 秒録画して保存し、自分で閉じる。 */
class CaptureActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var countdownText: TextView
    private lateinit var recordingDot: View

    // アプリ自身の Runnable 用。OverlayEffect には渡さない（onStop/onDestroy で全消去するため）。
    private val mainHandler = Handler(Looper.getMainLooper())

    // OverlayEffect 専用。ライブラリが GL 処理を積むので、アプリ側から消してはいけない。
    private val overlayHandler = Handler(Looper.getMainLooper())
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private var overlayEffect: OverlayEffect? = null
    private var awaitingPermission = false
    private var finishing = false
    private var streamObserver: Observer<PreviewView.StreamState>? = null

    private val delaySec: Int
        get() = if (intent?.action == ACTION_CAPTURE_DELAYED) DELAY_SEC else 0

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            awaitingPermission = false
            if (result.values.all { it }) startCamera() else finishWithToast(R.string.toast_permission_required)
        }

    // Finalize が来るまで recording は保持する（onStop が終了処理を Finalize に任せるため）。
    private val stopRecording = Runnable { recording?.stop() }

    // STREAMING にならなくても、一定時間で録画に進む。
    private val streamFallback = Runnable { onStreamReady() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_capture)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        previewView = findViewById(R.id.preview_view)
        countdownText = findViewById(R.id.countdown_text)
        recordingDot = findViewById(R.id.recording_dot)

        val granted = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            startCamera()
        } else {
            awaitingPermission = true
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    override fun onStop() {
        super.onStop()
        if (awaitingPermission || isChangingConfigurations) return
        mainHandler.removeCallbacksAndMessages(null)
        val active = recording
        if (active != null) {
            // 途中までの動画を保存する。終了は Finalize イベントで行う。
            active.stop()
        } else {
            finishWithToast(null)
        }
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        recording?.close()
        overlayEffect?.close()
        super.onDestroy()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (finishing) return@addListener
            try {
                val provider = future.get()
                val settings = SetcamSettings.from(PreferenceManager.getDefaultSharedPreferences(this).all)
                val stabilize = Recorder.getVideoCapabilities(provider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA))
                    .isStabilizationSupported
                videoCapture = try {
                    bindUseCases(provider, settings, stabilize)
                } catch (e: IllegalArgumentException) {
                    // 手ぶれ補正を要求していないなら、同じ失敗を繰り返さない。
                    if (!stabilize) throw e
                    Log.w(TAG, "binding with stabilization failed, retrying without it", e)
                    bindUseCases(provider, settings, stabilize = false)
                }
                awaitStreaming()
            } catch (e: Exception) {
                Log.e(TAG, "camera start failed", e)
                finishWithToast(R.string.toast_camera_failed)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindUseCases(
        provider: ProcessCameraProvider,
        settings: SetcamSettings,
        stabilize: Boolean,
    ): VideoCapture<Recorder> {
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        val quality = settings.resolution.toQuality()
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(quality)))
            .build()
        val videoCapture = VideoCapture.Builder(recorder)
            .setVideoStabilizationEnabled(stabilize)
            .build()
        val preview = Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .build(),
            )
            .build()
        preview.setSurfaceProvider(previewView.surfaceProvider)

        val group = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(videoCapture)
        overlayEffect?.close()
        overlayEffect = settings.stamp?.let { buildOverlay(it) }?.also { group.addEffect(it) }

        provider.unbindAll()
        provider.bindToLifecycle(this, selector, group.build())
        return videoCapture
    }

    /** プレビューと録画の両方のフレームに、正立した向きで日時を描く。 */
    private fun buildOverlay(style: StampStyle): OverlayEffect {
        val renderer = StampRenderer(this, style)
        val matrix = Matrix()
        val effect = OverlayEffect(
            CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE,
            0,
            overlayHandler,
        ) { t -> Log.e(TAG, "overlay effect error", t) }
        effect.setOnDrawListener { frame ->
            val canvas = frame.overlayCanvas
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            val crop = frame.cropRect
            val rotation = frame.rotationDegrees
            val upright = UprightTransform.uprightSize(crop.width(), crop.height(), rotation)
            matrix.setValues(UprightTransform.uprightToBuffer(crop.left, crop.top, crop.width(), crop.height(), rotation))
            canvas.save()
            canvas.concat(matrix)
            renderer.draw(canvas, upright.width, upright.height)
            canvas.restore()
            true
        }
        return effect
    }

    /** カメラが実際に映像を流し始めてから録画に進む（起動待ちで 2 秒を消費しない）。 */
    private fun awaitStreaming() {
        val observer = Observer<PreviewView.StreamState> { state ->
            if (state == PreviewView.StreamState.STREAMING) onStreamReady()
        }
        streamObserver = observer
        previewView.previewStreamState.observe(this, observer)
        mainHandler.postDelayed(streamFallback, STREAM_WAIT_MS)
    }

    /** STREAMING かフォールバックのうち、先に来た方で一度だけ進む。 */
    private fun onStreamReady() {
        val observer = streamObserver ?: return
        streamObserver = null
        previewView.previewStreamState.removeObserver(observer)
        mainHandler.removeCallbacks(streamFallback)
        if (finishing) return
        beginCountdown()
    }

    private fun beginCountdown() {
        if (delaySec == 0) {
            startRecording()
            return
        }
        var remaining = delaySec
        countdownText.visibility = View.VISIBLE
        val tick = object : Runnable {
            override fun run() {
                if (remaining == 0) {
                    countdownText.visibility = View.GONE
                    startRecording()
                    return
                }
                countdownText.text = remaining.toString()
                remaining--
                mainHandler.postDelayed(this, 1000)
            }
        }
        mainHandler.post(tick)
    }

    @SuppressLint("MissingPermission") // onCreate で CAMERA と RECORD_AUDIO を確認済み
    private fun startRecording() {
        val capture = videoCapture ?: return
        if (finishing) return
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, FileNames.videoFileName(LocalDateTime.now()))
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, RELATIVE_PATH)
        }
        try {
            val options = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(values)
                .build()
            recording = capture.output
                .prepareRecording(this, options)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), ::onRecordEvent)
        } catch (e: Exception) {
            Log.e(TAG, "recording start failed", e)
            finishWithToast(R.string.toast_camera_failed)
        }
    }

    private fun onRecordEvent(event: VideoRecordEvent) {
        when (event) {
            is VideoRecordEvent.Start -> {
                recordingDot.visibility = View.VISIBLE
                mainHandler.postDelayed(stopRecording, RECORD_DURATION_MS)
            }
            is VideoRecordEvent.Finalize -> {
                recording = null
                recordingDot.visibility = View.GONE
                val uri = event.outputResults.outputUri
                val saved = uri != Uri.EMPTY && (
                    event.error == VideoRecordEvent.Finalize.ERROR_NONE ||
                        event.error == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE
                    )
                if (!saved) Log.e(TAG, "recording failed: error=${event.error}", event.cause)
                finishWithToast(if (saved) R.string.toast_saved else R.string.toast_save_failed)
            }
        }
    }

    private fun finishWithToast(@StringRes message: Int?) {
        if (finishing) return
        finishing = true
        if (message != null) Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
        finishAndRemoveTask()
    }

    private fun Resolution.toQuality(): Quality = when (this) {
        Resolution.FHD -> Quality.FHD
        Resolution.UHD -> Quality.UHD
        Resolution.HD -> Quality.HD
    }

    companion object {
        const val ACTION_CAPTURE_DELAYED = "com.ktakata.setcam.action.CAPTURE_DELAYED"
        private const val TAG = "CaptureActivity"
        private const val DELAY_SEC = 3
        private const val RECORD_DURATION_MS = 2000L
        private const val STREAM_WAIT_MS = 3000L
        private const val RELATIVE_PATH = "Movies/setcam"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    }
}

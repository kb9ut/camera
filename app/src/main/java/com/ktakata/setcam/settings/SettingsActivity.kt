package com.ktakata.setcam.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ktakata.setcam.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // targetSdk 35 以上は edge-to-edge が強制されるので、システムバーの分だけ余白を取る。
        val toolbar = findViewById<View>(R.id.toolbar)
        val previewArea = findViewById<View>(R.id.preview_area)
        val container = findViewById<View>(R.id.settings_container)
        val previewPadding = previewArea.paddingLeft
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.settings_root)) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.setPadding(bars.left, bars.top, bars.right, 0)
            previewArea.setPadding(
                previewPadding + bars.left, previewPadding, previewPadding + bars.right, previewPadding,
            )
            container.setPadding(bars.left, 0, bars.right, bars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
    }
}

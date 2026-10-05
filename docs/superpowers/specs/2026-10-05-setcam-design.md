# setcam 設計仕様

- 日付: 2026-10-05
- ステータス: 承認済み

## 目的

Setlog のような「アイコンを押したら短い動画を撮って自動保存」する、自分用の記録カメラ Android アプリ。
共有機能は持たず、端末ローカルにのみ保存する。画質は CameraX で取れる範囲の最高画質を目指す。

## スコープ外（YAGNI）

- 共有・アップロード・クラウド連携
- アプリ内ギャラリー／再生画面（閲覧は端末のギャラリーアプリで行う）
- 前面カメラ、録画秒数・ディレイ秒数の変更
- 10bit HDR 録画

## 起動導線

| 操作 | 動作 |
|---|---|
| ランチャーアイコンをタップ | `CaptureActivity` を起動し、即座に 2 秒録画 → 保存 → 自動で閉じる |
| 長押しショートカット「3秒後に撮影」 | `CaptureActivity`（extra `delay_sec=3`）→ 3・2・1 カウントダウン → 2 秒録画 → 保存 → 閉じる |
| 長押しショートカット「設定」 | `SettingsActivity` を起動 |

- ショートカットは静的 App Shortcut（`res/xml/shortcuts.xml`）で定義する。
- ショートカットはホーム画面に固定すれば、2 つ目のアイコンとしても使える。

## 画面

### CaptureActivity

- 全画面プレビューと、録画中を示す赤丸インジケーター（ディレイ時はカウントダウン数字）だけを表示する。
- 操作ボタンは置かない。画面タップは無視する（誤タップで中断されない）。
- 完了したら Toast「保存しました」を出し、`finishAndRemoveTask()` で終了する。
- マニフェストで `excludeFromRecents="true"` とし、最近のアプリ一覧に残さない。
- 画面の向きは縦に固定する（記録用途なので向きによるブレをなくす）。

### SettingsActivity

AndroidX Preference を使い、値は `SharedPreferences` に保存する。3×3 の位置選択と、文字のプレビュー表示だけは自作 View にする。

| 項目 | 選択肢 | 既定値 |
|---|---|---|
| 解像度 | FHD（1080p）/ 4K（UHD）/ HD（720p） | FHD |
| 日時を表示 | ON / OFF | ON |
| 書式 | プリセット（`yyyy/M/d H:mm`、`yyyy-MM-dd HH:mm:ss`、`yy.MM.dd`、`M/d/yyyy h:mm a`）またはカスタム | `yyyy/M/d H:mm` |
| 位置 | 3×3 グリッド（9 か所） | 右下 |
| フォント | ゴシック / 明朝 / 等幅 / デジタル（DSEG7） | ゴシック |
| 色 | 白・黒・黄・オレンジ・赤・緑 | 白 |
| 大きさ | スライダー 2〜10%（フレーム短辺に対する割合） | 4% |

- 画面上部にプレビューを置き、灰色の背景に現在の設定で文字を描画する（`StampRenderer` を共用）。
- カスタム書式は `DateTimeFormatter.ofPattern` で検証する。不正なら入力欄にエラーを表示し、保存しない。
- フォントにデジタル（DSEG7）を選んだ場合、「数字と一部の記号のみ表示できます（英字や `/` は表示されません）」という注意書きを出す。

## 日時の焼き込み

- CameraX 1.4 の `OverlayEffect`（`androidx.camera:camera-effects`）を使う。対象は `CameraEffect.PREVIEW | CameraEffect.VIDEO_CAPTURE`。
- `UseCaseGroup.Builder().addUseCase(preview).addUseCase(videoCapture).addEffect(overlay)` でバインドする。
- GPU 上で合成し、ハードウェアエンコーダーへそのまま渡すため、後から再エンコードはしない（画質の劣化や保存待ちが発生しない）。
- 描画内容は**毎フレームの現在時刻**とする（秒を含む書式なら、録画中に表示が進む）。
- 向き: `frame.sensorToBufferTransform` と端末の回転から、「動画の回転メタデータが適用された後に正立して見える座標系」を求め、その座標系で 3×3 の位置を計算する。
- 文字サイズは `フレーム短辺 × サイズ%`。端からの余白は文字サイズの 1/2。
- 読みやすさのため、色の反対色で薄い影（shadowLayer）を付ける。
- `Paint` と `Typeface` は設定読み込み時に一度だけ生成し、`onDraw` の中では新しく作らない。
- 「日時を表示」が OFF のときは、`OverlayEffect` 自体をバインドしない。
- リスク: エフェクトを使うと、端末によっては手ブレ補正が無効になる場合がある。実機で確認し、併用できない端末では補正なしで動かす（録画を優先する）。
- DSEG7 フォントは `res/font/` に同梱する（SIL Open Font License 1.1。ライセンス文を `assets/licenses/` に置く）。

## 録画ロジック

```
onCreate
  → 権限チェック（CAMERA, RECORD_AUDIO）
      未許可 → リクエスト → 拒否されたら Toast で理由を出して finish
  → ProcessCameraProvider.bindToLifecycle(背面カメラ, Preview, VideoCapture)
  → delay_sec > 0 ならカウントダウン表示
  → recorder.prepareRecording(MediaStoreOutputOptions)
        .withAudioEnabled()
        .start()
  → VideoRecordEvent.Start を受けた時点から 2000ms 後に recording.stop()
  → VideoRecordEvent.Finalize
        エラーなし → Toast「保存しました」→ finishAndRemoveTask()
        エラーあり → Toast「保存に失敗しました」→ finishAndRemoveTask()
```

- **タイマー起点は `Start` イベント**。`start()` 呼び出し時点を起点にすると、エンコーダー起動までの時間が引かれて実際の尺が 2 秒未満になるため。
- 画質: `QualitySelector.from(設定値, FallbackStrategy.lowerQualityOrHigherThan(設定値))`。4K 非対応端末では自動的に FHD 以下へ落ちる。
- 手ブレ補正: `VideoCapture.Builder.setVideoStabilizationEnabled(true)`。`VideoCapabilities.isStabilizationSupported` が true の場合のみ有効にする。
- ビットレートは CameraX / ハードウェアエンコーダーの既定値を使う。
- 録画中に `onStop`（バックグラウンドへの移動、画面オフなど）が来たら、その時点で `stop()` して保存する（途中までの動画も残す）。

## 保存先

- `MediaStoreOutputOptions` で `MediaStore.Video.Media.EXTERNAL_CONTENT_URI` に書き込む。
- `RELATIVE_PATH = Movies/setcam`、ファイル名は `setcam_yyyyMMdd_HHmmss.mp4`（端末ローカル時刻）。
- Android 10 以上ではストレージ権限は不要。minSdk 26〜28 では `WRITE_EXTERNAL_STORAGE`（`maxSdkVersion=28`）を宣言する。
- 注意: Google フォト等のバックアップで `Movies/setcam` フォルダを有効にすると、クラウドにアップロードされる。既定では対象外。

## 権限

- `android.permission.CAMERA`
- `android.permission.RECORD_AUDIO`
- `android.permission.WRITE_EXTERNAL_STORAGE`（`maxSdkVersion="28"`）

## 技術構成

- Kotlin、単一 `app` モジュール
- minSdk 26 / targetSdk 35 / compileSdk 35
- CameraX 1.4 系（`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-video`, `camera-view`, `camera-effects`）
- AndroidX Preference（`preference-ktx`）
- UI は View ベース（Compose は使わない）
- パッケージ名: `com.ktakata.setcam`
- UI 文言は日本語（`res/values/strings.xml`）

## コンポーネント分割

| ファイル | 責務 |
|---|---|
| `CaptureActivity.kt` | 権限、カメラのバインド、カウントダウン、録画の開始・停止、終了処理 |
| `SettingsActivity.kt` / `SettingsFragment.kt` | 設定画面の UI（Preference） |
| `PositionGridPreference.kt` | 3×3 の位置選択 UI |
| `StampPreviewPreference.kt` | 設定画面上部のプレビュー |
| `SetcamSettings.kt` | `SharedPreferences` の読み書き。設定値 → `Quality` / `StampStyle` への変換 |
| `StampStyle.kt` | 書式・位置・フォント・色・大きさをまとめた不変データクラス |
| `StampLayout.kt` | フレームサイズ・位置・文字サイズから描画座標を求める（純粋関数） |
| `StampRenderer.kt` | `Canvas` に日時を描画する。`OverlayEffect` とプレビューの両方から使う |
| `FileNames.kt` | 日時 → ファイル名の生成（純粋関数） |

## エラー処理

- 権限拒否: Toast で「カメラとマイクの許可が必要です」と出して終了する。
- カメラのバインドに失敗（他アプリが使用中など）: Toast で「カメラを起動できませんでした」と出して終了する。
- `Finalize` でエラー: Toast で「保存に失敗しました」と出して終了する。ただし `ERROR_SOURCE_INACTIVE` などで中断されてもファイルが残る場合は、保存扱いにする。

## テスト

- ユニットテスト（JVM）: `FileNames`（日時 → 名前）、`SetcamSettings` の設定値 → `Quality` / `StampStyle` 変換、`StampLayout` の 9 か所それぞれの座標計算（横長・縦長フレーム、回転 0/90/180/270）、カスタム書式の検証
- 最初の試作（スパイク）: `OverlayEffect` で FHD と 4K の録画に文字が正しい向き・位置で入ること、および手ブレ補正と併用できるかを実機で確認する
- 手動確認（実機またはエミュレーター）:
  1. アイコンをタップ → 約 2 秒の動画が `Movies/setcam` に保存され、アプリが閉じる
  2. 長押し →「3秒後に撮影」→ カウントダウン後に録画・保存される
  3. 設定で 4K を選ぶ → 対応端末では 2160p、非対応端末では 1080p 以下で保存される
  4. 録画中にホームボタンを押す → 途中までの動画が保存される
  5. 権限を拒否する → 理由の Toast が出て終了する
  6. 日時の表示 ON → 保存した動画の指定位置に、指定した書式・フォント・色・大きさで日時が正立して入っている
  7. 日時の表示 OFF → 文字が入っていない
  8. 設定画面で各項目を変える → プレビューがすぐに更新される

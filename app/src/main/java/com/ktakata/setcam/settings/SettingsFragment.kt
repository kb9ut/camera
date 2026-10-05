package com.ktakata.setcam.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.ktakata.setcam.R
import com.ktakata.setcam.stamp.DateFormats
import com.ktakata.setcam.stamp.StampFont
import com.ktakata.setcam.stamp.StampStyle
import java.time.ZonedDateTime

class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setupFormatPreset()
        setupCustomFormat()
        setupFont()
        refresh()
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        refresh()
    }

    override fun onPause() {
        preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(prefs: SharedPreferences?, key: String?) = refresh()

    /** プリセットの選択肢は「今の日時で整形した例」として表示する。 */
    private fun setupFormatPreset() {
        val pref = requirePreference<ListPreference>(SetcamSettings.KEY_FORMAT_PRESET)
        val now = ZonedDateTime.now()
        pref.entries = (SetcamSettings.PRESET_PATTERNS.map { DateFormats.formatOrNull(it, now) ?: it } +
            getString(R.string.pref_format_custom)).toTypedArray()
        pref.entryValues = (SetcamSettings.PRESET_PATTERNS + SetcamSettings.PRESET_CUSTOM).toTypedArray()
    }

    private fun setupCustomFormat() {
        val pref = requirePreference<EditTextPreference>(SetcamSettings.KEY_FORMAT_CUSTOM)
        pref.setOnBindEditTextListener { edit ->
            edit.isSingleLine = true
            edit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    edit.error = if (DateFormats.isValid(s?.toString())) null else getString(R.string.error_invalid_format)
                }
            })
        }
        pref.setOnPreferenceChangeListener { _, newValue ->
            val ok = DateFormats.isValid(newValue as? String)
            if (!ok) Toast.makeText(requireContext(), R.string.error_invalid_format, Toast.LENGTH_SHORT).show()
            ok
        }
        pref.summaryProvider = Preference.SummaryProvider<EditTextPreference> { p ->
            val sample = DateFormats.formatOrNull(p.text, ZonedDateTime.now())
            if (sample == null) getString(R.string.error_invalid_format) else "${p.text}  →  $sample"
        }
    }

    private fun setupFont() {
        requirePreference<ListPreference>(SetcamSettings.KEY_FONT).summaryProvider =
            Preference.SummaryProvider<ListPreference> { p ->
                if (p.value == StampFont.DIGITAL.name) getString(R.string.pref_font_digital_note, p.entry) else p.entry
            }
    }

    private fun refresh() {
        val prefs = preferenceManager.sharedPreferences ?: return
        val isCustom = prefs.getString(SetcamSettings.KEY_FORMAT_PRESET, StampStyle.DEFAULT_PATTERN) ==
            SetcamSettings.PRESET_CUSTOM
        requirePreference<EditTextPreference>(SetcamSettings.KEY_FORMAT_CUSTOM).isVisible = isCustom
        requireActivity().findViewById<StampPreviewView>(R.id.stamp_preview)?.style =
            SetcamSettings.from(prefs.all).stamp
    }

    private fun <T : Preference> requirePreference(key: String): T =
        requireNotNull(findPreference<T>(key)) { "preference not found: $key" }
}

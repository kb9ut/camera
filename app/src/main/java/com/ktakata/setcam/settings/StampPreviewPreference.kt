package com.ktakata.setcam.settings

import android.content.Context
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.ktakata.setcam.R
import com.ktakata.setcam.stamp.StampStyle

class StampPreviewPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {

    init {
        layoutResource = R.layout.pref_stamp_preview
        isSelectable = false
        isPersistent = false
    }

    /** null なら日時を表示しない（灰色の枠だけ）。 */
    var style: StampStyle? = null
        set(value) {
            if (field != value) {
                field = value
                notifyChanged()
            }
        }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        (holder.findViewById(R.id.stamp_preview) as StampPreviewView).style = style
    }
}

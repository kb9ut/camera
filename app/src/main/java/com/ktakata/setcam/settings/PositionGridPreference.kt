package com.ktakata.setcam.settings

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.ktakata.setcam.R
import com.ktakata.setcam.stamp.StampPosition

/** StampPosition の名前（例: "BOTTOM_RIGHT"）を文字列として保存する。 */
class PositionGridPreference @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : Preference(context, attrs) {

    private var value: String = StampPosition.BOTTOM_RIGHT.name

    init {
        layoutResource = R.layout.pref_position_grid
        isSelectable = false
    }

    override fun onGetDefaultValue(a: TypedArray, index: Int): Any? = a.getString(index)

    override fun onSetInitialValue(defaultValue: Any?) {
        value = getPersistedString(defaultValue as? String ?: value)
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        val grid = holder.findViewById(R.id.position_grid) as PositionGridView
        grid.isEnabled = isEnabled
        grid.selected = StampPosition.entries.firstOrNull { it.name == value } ?: StampPosition.BOTTOM_RIGHT
        grid.onSelect = { position ->
            if (callChangeListener(position.name)) {
                value = position.name
                persistString(position.name)
                grid.selected = position
            }
        }
    }
}

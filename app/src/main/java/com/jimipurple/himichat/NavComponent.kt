package com.jimipurple.himichat

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.nav_component.view.*

class NavComponent @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
    defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyle, defStyleRes) {
    var settingsButton: ImageButton? = null
    var dialoguesButton: ImageButton? = null
    var friendsButton: ImageButton? = null

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.nav_component, this, true)

        orientation = HORIZONTAL
        settingsButton = navSettingsButton
        dialoguesButton = navDialoguesButton
        friendsButton = navFriendsButton
    }
}

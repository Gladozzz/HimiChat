package com.jimipurple.himichat.adapters

import android.content.Context
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.jimipurple.himichat.DesignSettingsFragment
import com.jimipurple.himichat.ProfileSettingsFragment
import com.jimipurple.himichat.R


class SettingsPageAdapter(fm: FragmentManager, context: Context, private val logoutCallback: () -> Unit, private val loadAvatarCallback: () -> Unit) : FragmentPagerAdapter(fm) {
    private var cont: Context = context
//    private val logout = logoutCallback
//    private val loadAvatar = loadAvatarCallback

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                ProfileSettingsFragment(logoutCallback, loadAvatarCallback)
            }
            else -> {
                return DesignSettingsFragment()
            }
        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> cont.getString(R.string.profile)
            else -> {
                return cont.getString(R.string.design)
            }
        }
    }
}
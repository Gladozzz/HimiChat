package com.jimipurple.himichat.adapters

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.jimipurple.himichat.ui.settings.DesignSettingsFragment
import com.jimipurple.himichat.ui.settings.PrivacySettingsFragment
import com.jimipurple.himichat.ui.settings.ProfileSettingsFragment
import com.jimipurple.himichat.R


class SettingsPageAdapter(fm: FragmentManager, context: Context, private val logoutCallback: () -> Unit, private val loadAvatarCallback: () -> Unit) : FragmentPagerAdapter(fm,FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private var cont: Context = context
    private var mCurrentFragment: Fragment? = null
//    private val logout = logoutCallback
//    private val loadAvatar = loadAvatarCallback


    fun getCurrentFragment(): Fragment? {
        return mCurrentFragment
    }

    override fun setPrimaryItem(container: ViewGroup, position: Int, `object`: Any) {
        super.setPrimaryItem(container, position, `object`)
        if (getCurrentFragment() !== `object`) {
            mCurrentFragment = `object` as Fragment
        }
        super.setPrimaryItem(container, position, `object`)
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                ProfileSettingsFragment(
                    logoutCallback,
                    loadAvatarCallback
                )
            }
            1 -> {
                PrivacySettingsFragment()
            }
            else -> {
                return DesignSettingsFragment()
            }
        }
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> cont.getString(R.string.profile)
            1 -> cont.getString(R.string.privacy)
            else -> {
                return cont.getString(R.string.design)
            }
        }
    }
}
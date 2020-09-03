package com.jimipurple.himichat.ui.settings


import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.jimipurple.himichat.R
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import kotlinx.android.synthetic.main.design_settings_fragment.*


class DesignSettingsFragment : Fragment() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()
    private var currentTheme: Boolean = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sp = requireContext().applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
        currentTheme = sp.getBoolean("night_mode", false)
        when (currentTheme) {
            true -> {
                switchTheme.isChecked = true
                sp.edit().putBoolean("night_mode", true).apply()
            }
            false -> {
                switchTheme.isChecked = false
                sp.edit().putBoolean("night_mode", false).apply()
            }
        }
        mAuth = FirebaseAuth.getInstance()
        switchTheme.setOnCheckedChangeListener { button, b ->
            when (b) {
                true -> {
                    sp.edit().putBoolean("night_mode", true).apply()
                    (requireContext() as Activity).setTheme(R.style.NightTheme)
                    (requireContext() as Activity).recreate()
                }
                false -> {
                    sp.edit().putBoolean("night_mode", false).apply()
                    (requireContext() as Activity).setTheme(R.style.DayTheme)
                    (requireContext() as Activity).recreate()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.design_settings_fragment, container, false)
    }
}

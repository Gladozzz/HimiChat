package com.jimipurple.himichat

import android.annotation.SuppressLint
//import android.app.ProgressDialog
import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.data.FirebaseSource


@SuppressLint("Registered")
open class BaseFragment : Fragment() {

    protected val cacheSize = 200 * 1024 * 1024 // 200MiB
    protected var c : Context? = null
    protected var mAuth: FirebaseAuth? = null
    protected var firestore: FirebaseFirestore? = null
    protected var firebaseToken: String  = ""
    protected var functions: FirebaseFunctions? = null
    protected var app: MyApp? = null
    protected var ac: AppCompatActivity? = null
    protected var bar: ActionBar? = null
    protected var fbSource: FirebaseSource? = null
    var tbar: Toolbar? = null
    var title: CharSequence? = null
    var subtitle: CharSequence? = null
    var navOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)  // Used to prevent multiple copies of the same destination
        .setEnterAnim(com.jimipurple.himichat.R.animator.fragment_fade_in)
        .setPopEnterAnim(com.jimipurple.himichat.R.animator.fragment_fade_in)
        .build()
    protected var currentTheme: Resources.Theme? = null

//    protected var mMyApp: MyApp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app = this.c!!.applicationContext as MyApp
        fbSource = FirebaseSource(c!!)
        tbar = app!!.tbar
        val sp = c!!.applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
        val darkMode = sp.getBoolean("night_mode", false)
        currentTheme = when (darkMode) {
            true -> {
                sp.edit().putBoolean("night_mode", true).apply()
                ContextThemeWrapper(c!!, com.jimipurple.himichat.R.style.NightTheme).theme
            }
            false -> {
                sp.edit().putBoolean("night_mode", false).apply()
                ContextThemeWrapper(c!!, com.jimipurple.himichat.R.style.DayTheme).theme
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        if (savedInstanceState != null) {
//            val title = savedInstanceState.getCharSequence("title")
//            val subtitle = savedInstanceState.getCharSequence("subtitle")
//            tbar!!.title = title
//            tbar!!.subtitle = subtitle
//            Log.i("savedTitle", "title $title subtitle $subtitle")
//        }
    }

    override fun onStart() {
        super.onStart()
//        FirebaseApp.initializeApp(c!!)
//        mAuth = FirebaseAuth.getInstance()
//        firestore = FirebaseFirestore.getInstance()
//        functions = FirebaseFunctions.getInstance()
//        firebaseToken = c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")!!

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        c = context
        app = this.c!!.applicationContext as MyApp
        tbar = app!!.tbar
        FirebaseApp.initializeApp(c!!)
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()
        firebaseToken = c!!.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")!!
    }

//    @VisibleForTesting
//    val progressDialog by lazy {
//        ProgressDialog(c)
//    }

//    override fun onResume() {
//        super.onResume()
//        mMyApp!!.currentActivity = this
//    }

//    override fun onPause() {
//        clearReferences()
//        super.onPause()
//    }

//    override fun onDestroy() {
//        clearReferences()
//        super.onDestroy()
//    }

//    private fun clearReferences() {
//        val currActivity = mMyApp!!.currentActivity
//        if (this == currActivity) mMyApp!!.currentActivity = null
//    }

//    fun showProgressDialog() {
//        progressDialog.setMessage(getString(R.string.loading))
//        progressDialog.isIndeterminate = true
//        progressDialog.show()
//    }

//    private fun hideProgressDialog() {
//        if (progressDialog.isShowing) {
//            progressDialog.dismiss()
//        }
//    }

    fun hideKeyboard(view: View) {
        val imm = c!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
//        outState.putCharSequence("title", tbar!!.title)
//        outState.putCharSequence("subtitle", tbar!!.subtitle)
//        val title = tbar!!.title
//        val subtitle = tbar!!.subtitle
//        Log.i("savedTitle", "title $title subtitle $subtitle")
//        outState.putSerializable("logo", toolbar!!.logo)
    }

//    public override fun onStop() {
//        super.onStop()
//        hideProgressDialog()
//    }
}

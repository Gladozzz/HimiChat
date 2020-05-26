package com.jimipurple.himichat

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.util.VisibleForTesting
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso


@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity() {

    protected val cacheSize = 200 * 1024 * 1024 // 200MiB
    protected var mAuth: FirebaseAuth? = null
    protected var firestore: FirebaseFirestore? = null
    protected var firebaseToken: String  = ""
    protected var functions: FirebaseFunctions? = null

    protected var mMyApp: MyApp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(applicationContext)
        mAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance()
        firebaseToken = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")!!

        mMyApp = this.applicationContext as MyApp
        mMyApp!!.currentActivity = this
    }

    @VisibleForTesting
    val progressDialog by lazy {
        ProgressDialog(this)
    }

    override fun onResume() {
        super.onResume()
        mMyApp!!.currentActivity = this
    }

    override fun onStart() {
        super.onStart()
        mMyApp!!.currentActivity = this
    }

    override fun onPause() {
//        clearReferences()
        super.onPause()
    }

    override fun onDestroy() {
//        clearReferences()
        super.onDestroy()
    }

    private fun clearReferences() {
        val currActivity = mMyApp!!.currentActivity
        if (this == currActivity) mMyApp!!.currentActivity = null
    }

    fun showProgressDialog() {
        progressDialog.setMessage(getString(R.string.loading))
        progressDialog.isIndeterminate = true
        progressDialog.show()
    }

    private fun hideProgressDialog() {
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
    }

    fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    public override fun onStop() {
        super.onStop()
        hideProgressDialog()
    }
}

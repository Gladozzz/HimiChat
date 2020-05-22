package com.jimipurple.himichat

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.appcompat.widget.Toolbar
import com.google.firebase.FirebaseApp
import com.squareup.picasso.Picasso


class MyApp : Application() {

    var currentActivity: Activity? = null
    var tbar: Toolbar? = null
    val Context.myApp: MyApp
        get() = applicationContext as MyApp
    override fun onCreate() {
        super.onCreate()
        Picasso.setSingletonInstance(Picasso.Builder(this).build())
        FirebaseApp.initializeApp(this)
    }
}
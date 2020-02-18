package com.jimipurple.himichat

import android.app.Activity
import android.app.Application
import com.google.firebase.FirebaseApp
import com.squareup.picasso.Picasso


class MyApp : Application() {

    var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
        Picasso.setSingletonInstance(Picasso.Builder(this).build())
        FirebaseApp.initializeApp(this)
    }
}
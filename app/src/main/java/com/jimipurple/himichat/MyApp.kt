package com.jimipurple.himichat

import android.app.Activity

import android.app.Application


class MyApp : Application() {

    var currentActivity: Activity? = null

    override fun onCreate() {
        super.onCreate()
    }
}
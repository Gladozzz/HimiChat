package com.jimipurple.himichat

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import com.google.firebase.FirebaseApp
import com.squareup.picasso.Picasso


class MyApp : Application() {

    var currentActivity: BaseActivity? = null
    var tbar: Toolbar? = null
    var bar: ActionBar? = null
    var title: String? = null
    var subtitle: String? = null
    var avatar: String? = null
    val Context.myApp: MyApp
        get() = applicationContext as MyApp
    var logoutCallback: () -> Unit = {}
    var loadAvatarCallback: () -> Unit = {}

    override fun onCreate() {
        super.onCreate()
        Picasso.setSingletonInstance(Picasso.Builder(this).build())
        FirebaseApp.initializeApp(this)
    }

    fun setToolbar(newTitle: String?, newAvatar: String?) {
        title = newTitle
        avatar = newAvatar
    }
}
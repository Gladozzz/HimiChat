package com.jimipurple.himichat

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.navigation.NavigationView
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.jimipurple.himichat.models.User
import com.jimipurple.himichat.ui.settings.SettingsFragment
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import net.sectorsieteg.avatars.AvatarDrawableFactory
import java.io.ByteArrayOutputStream
import java.io.File
import com.squareup.picasso.LruCache as PicLruCache

class NavigationActivity : BaseActivity() {

    private val tag = "NavigationActivity"

    private val NAVIGATE_TO_SENDER = 6
    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3
    private var currentTheme: Boolean = false

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var nickname: String? = null
    private var realname: String? = null
    private var navCon: NavController? = null
    var navOptions = NavOptions.Builder()
        .setLaunchSingleTop(true)  // Used to prevent multiple copies of the same destination
        .setEnterAnim(com.jimipurple.himichat.R.animator.fragment_fade_in)
        .setPopEnterAnim(com.jimipurple.himichat.R.animator.fragment_fade_in)
        .build()

    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isMyServiceRunning(SocketService::class.java)) {
            startService(Intent(applicationContext, SocketService::class.java))
        }
        val sp = applicationContext.getSharedPreferences("com.jimipurple.himichat.prefs", 0)
        currentTheme = sp.getBoolean("night_mode", false)
        when (currentTheme) {
            true -> {
                sp.edit().putBoolean("night_mode", true).apply()
                setTheme(R.style.NightTheme)
            }
            false -> {
                sp.edit().putBoolean("night_mode", false).apply()
                setTheme(R.style.DayTheme)
            }
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_navigation)

        val toolbar: Toolbar? = findViewById(R.id.mytoolbar)
        setSupportActionBar(toolbar)
        overridePendingTransition(R.animator.fragment_fade_in, R.animator.fragment_fade_in)


//        val fab: FloatingActionButton = findViewById(R.id.fab)
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        navCon = navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            navController.graph, drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.getHeaderView(0)
        updateAvatar()

        fbSource!!.isAdmin(fbSource!!.uid()!!, { isAdmin ->
            if (isAdmin) {
                navView.menu.findItem(R.id.nav_friends).isVisible = false
                navView.menu.findItem(R.id.nav_users).isVisible = true
            }
        })

        val bar = supportActionBar
        bar!!.setCustomView(R.layout.custom_toolbar)
        if (savedInstanceState != null) {
            val title = savedInstanceState.getCharSequence("title")
            val subtitle = savedInstanceState.getCharSequence("subtitle")
            Log.i("savedTitle", "onCreate title $title")
            bar!!.title = title
            bar.subtitle = subtitle
        }
        navView.setNavigationItemSelectedListener { item ->
            item.isChecked = true
            navController.navigate(item.itemId, null, navOptions)
            drawerLayout.closeDrawers()
            true
        }
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            Log.i("navController", "called " + destination.label)
            when(destination.id) {
                R.id.nav_dialogues -> {
                    bar!!.setTitle(R.string.menu_dialogues)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_dialog -> {
                    Log.i("navController", "arg " + arguments?.get("nickname"))
                    Log.i("navController", "arg " + arguments?.get("avatar"))
                    bar!!.setTitle(R.string.menu_dialog)
                    val title = arguments!!["nickname"] as String
                    val avatar = arguments["avatar"] as String
                    bar.title = title
                    bar.subtitle = resources.getString(R.string.offline)
                    val url = Uri.parse(avatar)
                    if (url != null) {
                        val bitmap = com.squareup.picasso.LruCache(applicationContext)[avatar]
                        if (bitmap != null) {
                            bar.setLogo(BitmapDrawable(bitmap))
                        } else {
                            Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                                override fun onBitmapLoaded(
                                    bitmap: Bitmap?,
                                    from: Picasso.LoadedFrom?
                                ) {
                                    com.squareup.picasso.LruCache(applicationContext!!).set(
                                        avatar,
                                        bitmap!!
                                    )
                                    val options = BitmapFactory.Options()
                                    options.inMutable = false
                                    val avatarFactory = AvatarDrawableFactory(resources)
                                    val avatarDrawable =
                                        avatarFactory.getRoundedAvatarDrawable(bitmap)
                                    bar.setLogo(avatarDrawable)
                                }

                                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                                override fun onBitmapFailed(
                                    e: Exception?,
                                    errorDrawable: Drawable?
                                ) {
                                    Log.i(
                                        "Profile",
                                        "Загрузка изображения не удалась " + url + "\n" + e?.message
                                    )
                                }
                            })
                        }
                    } else {
                        Log.i("Profile", "avatar wasn't received")
                    }
                }
                R.id.nav_find_friend -> {
                    bar!!.setTitle(R.string.menu_find_friend)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_friend_requests -> {
                    bar!!.setTitle(R.string.menu_friend_requests)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_profile -> {
                    bar!!.setTitle(R.string.menu_profile)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_friends -> {
                    bar!!.setTitle(R.string.menu_friends)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_settings -> {
                    bar!!.setTitle(R.string.menu_settings)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
            }
        }
        mMyApp!!.currentActivity = this
        mMyApp!!.tbar = toolbar
        mMyApp!!.bar = bar

        val extras = intent.extras
        if (extras != null) {
            val sender = intent.getBundleExtra("sender")
            if (sender != null) {
                val senderName = sender.getString("nickname")
                val senderId = sender.getString("friend_id")
                Log.i("NAVIGATE_TO_SENDER", "senderId $senderId")
                val senderAvatar = sender.getString("avatar")
                val b = Bundle()
                b.putString("friend_id", senderId)
                b.putString("nickname", senderName)
                b.putString("avatar", senderAvatar)
                navCon!!.navigate(R.id.nav_dialog, b, navOptions)
            } else {
                Log.i("NAVIGATE_TO_SENDER", "Bundle is null")
            }
        } else {
            Log.i("NAVIGATE_TO_SENDER", "Extras is null")
        }
    }

    fun setUpNavViewOnUser() {

    }

    fun setUpNavViewOnAdmin(savedInstanceState: Bundle?) {
        val toolbar: Toolbar? = findViewById(R.id.mytoolbar)
        setSupportActionBar(toolbar)
        overridePendingTransition(R.animator.fragment_fade_in, R.animator.fragment_fade_in)
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        navCon = navController
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            navController.graph, drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.getHeaderView(0)
        updateAvatar()

        val bar = supportActionBar
        if (savedInstanceState != null) {
            val title = savedInstanceState.getCharSequence("title")
            val subtitle = savedInstanceState.getCharSequence("subtitle")
            Log.i("savedTitle", "onCreate title $title")
            bar!!.title = title
            bar.subtitle = subtitle
        }
        navView.setNavigationItemSelectedListener { item ->
            item.isChecked = true
            navController.navigate(item.itemId, null, navOptions)
            drawerLayout.closeDrawers()
            true
        }
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            Log.i("navController", "called " + destination.label)
            when(destination.id) {
                R.id.nav_dialogues -> {
                    bar!!.setTitle(R.string.menu_dialogues)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_dialog -> {
                    Log.i("navController", "arg " + arguments?.get("nickname"))
                    Log.i("navController", "arg " + arguments?.get("avatar"))
                    bar!!.setTitle(R.string.menu_dialog)
                    val title = arguments!!["nickname"] as String
                    val avatar = arguments["avatar"] as String
                    bar.title = title
                    bar.subtitle = resources.getString(R.string.offline)
                    val url = Uri.parse(avatar)
                    if (url != null) {
                        val bitmap = com.squareup.picasso.LruCache(applicationContext)[avatar]
                        if (bitmap != null) {
                            bar.setLogo(BitmapDrawable(bitmap))
                        } else {
                            Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                                override fun onBitmapLoaded(
                                    bitmap: Bitmap?,
                                    from: Picasso.LoadedFrom?
                                ) {
                                    com.squareup.picasso.LruCache(applicationContext!!).set(
                                        avatar,
                                        bitmap!!
                                    )
                                    bar.setLogo(BitmapDrawable(bitmap))
                                }

                                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                                override fun onBitmapFailed(
                                    e: Exception?,
                                    errorDrawable: Drawable?
                                ) {
                                    Log.i(
                                        "Profile",
                                        "Загрузка изображения не удалась " + url + "\n" + e?.message
                                    )
                                }
                            })
                        }
                    } else {
                        Log.i("Profile", "avatar wasn't received")
                    }
                }
                R.id.nav_find_friend -> {
                    bar!!.setTitle(R.string.menu_find_friend)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_friend_requests -> {
                    bar!!.setTitle(R.string.menu_friend_requests)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_profile -> {
                    bar!!.setTitle(R.string.menu_profile)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_friends -> {
                    bar!!.setTitle(R.string.menu_friends)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_users -> {
                    bar!!.setTitle(R.string.menu_users)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
                R.id.nav_settings -> {
                    bar!!.setTitle(R.string.menu_settings)
                    bar.subtitle = ""
                    bar.setLogo(null)
                }
            }
        }
        mMyApp!!.currentActivity = this
        mMyApp!!.tbar = toolbar
        mMyApp!!.bar = bar

        val extras = intent.extras
        if (extras != null) {
            val sender = intent.getBundleExtra("sender")
            if (sender != null) {
                val senderName = sender.getString("nickname")
                val senderId = sender.getString("friend_id")
                Log.i("NAVIGATE_TO_SENDER", "senderId $senderId")
                val senderAvatar = sender.getString("avatar")
                val b = Bundle()
                b.putString("friend_id", senderId)
                b.putString("nickname", senderName)
                b.putString("avatar", senderAvatar)
                navCon!!.navigate(R.id.nav_dialog, b, navOptions)
            } else {
                Log.i("NAVIGATE_TO_SENDER", "Bundle is null")
            }
        } else {
            Log.i("NAVIGATE_TO_SENDER", "Extras is null")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.navigation, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun updateAvatar(){
        val navView: NavigationView = findViewById(R.id.nav_view)
        val parentView = navView.getHeaderView(0).rootView

        val navHeaderNicknameView = parentView.findViewById<TextView>(R.id.navHeaderNicknameView)
        val navHeaderRealnameView = parentView.findViewById<TextView>(R.id.navHeaderRealnameView)
        val navHeaderAvatarView = parentView.findViewById<ImageView>(R.id.navHeaderAvatarView)

        val pref = SharedPreferencesUtility(applicationContext)
        nickname = pref.getString("nickname")
        realname = pref.getString("realname")
        navHeaderNicknameView.text = nickname
        navHeaderRealnameView.text = realname
        val avatar = pref.getString("avatar")
        if (avatar != null) {
            val bitmap = PicLruCache(applicationContext)[avatar]
            if (bitmap != null) {
                navHeaderAvatarView.setImageBitmap(bitmap)
                Log.i("navProfile", "avatar was loaded from cache")
            } else {
                Log.e("navProfile", "Avatar is not in cache. Preload can be done")
            }
        } else {
            Log.e("navProfile", "avatar was not in SharedPrefences")
        }

        fbSource!!.getUser(mAuth!!.uid!!, { user ->
            nickname = user.nickname
            realname = user.realName
            val newAvatar = user.avatar
            pref.putString("nickname", nickname!!)
            pref.putString("realname", realname!!)
            pref.putString("avatar", newAvatar)
            val url = Uri.parse(newAvatar)
            if (url != null) {
                val bitmap = PicLruCache(applicationContext)[newAvatar]
                if (bitmap != null) {
                    navHeaderAvatarView.setImageBitmap(bitmap)
                    Log.i("navProfile", "avatar was loaded from cache")
                } else {
                    Log.i("navProfile", "avatar will load from internet")
                    val b = Glide.with(this)
                        .asBitmap()
                        .load(url)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
                                PicLruCache(applicationContext).set(newAvatar, resource)
//                                val options = BitmapFactory.Options()
//                                options.inMutable = false
//                                val avatarFactory = AvatarDrawableFactory(resources)
//                                val avatarDrawable =
//                                    avatarFactory.getRoundedAvatarDrawable(resource)
                                navHeaderAvatarView.setImageBitmap(resource)
                                Log.i(
                                    "navProfile",
                                    "bitmap from $url is loaded and set to imageView"
                                )
                            }

                            override fun onLoadCleared(placeholder: Drawable?) {
                                // this is called when imageView is cleared on lifecycle call or for
                                // some other reason.
                                // if you are referencing the bitmap somewhere else too other than this imageView
                                // clear it here as you can no longer have the bitmap
                                navHeaderAvatarView.setImageBitmap(
                                    resources.getDrawable(R.drawable.defaultavatar).toBitmap()
                                )
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                super.onLoadFailed(errorDrawable)
                                Log.e("navProfile", "Загрузка изображения не удалась $url")
                            }
                        })
                }
            } else {
                Log.e("navProfile", "avatar wasn't received")
            }
        })
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("SettingsActivity", "Permission to read storage denied")
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
        )
    }

    override fun onStart() {
        super.onStart()
        fun usersToStrings(h: ArrayList<User>) : ArrayList<String> {
            val s : ArrayList<String> = ArrayList<String>()
            h.forEach {
                val gson = Gson()
                val json = gson.toJson(it)
                s.add(json)
            }
            return s
        }
        val pref = SharedPreferencesUtility(applicationContext)
        fbSource!!.getUser(fbSource!!.uid()!!, { user: User ->
            Log.i("friendsTest", "friends ${user.friends}")
            if (user.friends != null && user.friends!!.isNotEmpty())
                user.friends?.let {
                    fbSource!!.getUsers(it, { users: List<User>? ->
                        val strings = usersToStrings(users as ArrayList<User>)
                        pref.putListString("friends", strings)
                        Log.i("friendsTest", "friends was took from server")
                    })
                }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(tag, "onActivityResult $requestCode $resultCode $data")
        when (requestCode) {
            NAVIGATE_TO_SENDER -> {
                val sender = data!!.getBundleExtra("sender")!!
                val senderName = sender.getString("nickname")
                val senderId = sender.getString("friend_id")
                Log.i("NAVIGATE_TO_SENDER", "onActivityResult senderId $senderId")
                val senderAvatar = sender.getString("avatar")
                val b = Bundle()
                b.putString("friend_id", senderId)
                b.putString("nickname", senderName)
                b.putString("avatar", senderAvatar)
                navCon!!.navigate(R.id.nav_dialog, b, navOptions)
                return
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val bar = supportActionBar!!
        Log.i("savedTitle", "osis savedTitle " + bar.title + " savedSubtitle " + bar.subtitle)
        outState!!.putCharSequence("title", bar.title)
        outState!!.putCharSequence("subtitle", bar.subtitle)
//        outState?.putSerializable("logo", toolbar!!.logo)
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>, grantResults: IntArray
//    ) {
//        when (requestCode) {
//            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//
//                    // start Activity if granted
//                    startActivityForResult(
//                        Intent(
//                            Intent.ACTION_PICK,
//                            MediaStore.Images.Media.INTERNAL_CONTENT_URI
//                        ), RESULT_LOAD_IMAGE
//                    )
//
//                } else {
//                    Log.i("SettingsActivity", " no permission to load avatar from storage")
//                    // permission denied, boo! Disable the
//                    // functionality that depends on this permission.
//                }
//                return
//            }
//        }// other 'case' lines to check for other
//        // permissions this app might request
//    }
}

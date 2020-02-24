package com.jimipurple.himichat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.storage.FirebaseStorage
import com.jimipurple.himichat.models.User
import com.google.gson.Gson
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import com.squareup.picasso.LruCache as PicLruCache

class NavigationActivity : BaseActivity() {

    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3

    private lateinit var appBarConfiguration: AppBarConfiguration
    private var nickname: String? = null
    private var realname: String? = null
    private var storage: FirebaseStorage? = null

    private fun hashMapToUser(h : ArrayList<java.util.HashMap<String, Any>>) : ArrayList<User> {
        val u : ArrayList<User> = ArrayList<User>()
        h.forEach {
            u.add(User(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
        }
        Log.i("convert", h.toString())
        Log.i("convert", u.toString())
        return u
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        storage = FirebaseStorage.getInstance()

        startService(Intent(this, SocketService::class.java))

//        val data = mapOf("id" to mAuth!!.uid!!)
//        functions!!
//            .getHttpsCallable("getFriends")
//            .call(data).continueWith { task ->
//                val result = task.result?.data as java.util.HashMap<String, Any>
//                if (result["found"] as Boolean) {
//                    val friends = result["friends"] as ArrayList<String>
//                    val data1 = mapOf("ids" to friends)
//                    functions!!
//                        .getHttpsCallable("getUsers")
//                        .call(data1).continueWith { task ->
//                            val result1 = task.result?.data as java.util.HashMap<String, Any>
//                            if (result1["found"] == true) {
//                                val users = result1["users"] as ArrayList<java.util.HashMap<String, Any>>
//                                val unfound = result1["unfound"] as ArrayList<String>
//                                val arr = hashMapToUser(users)
//                                LruCache<String, ArrayList<User>>(cacheSize).put("friends", arr)
//                            }
//                        }
//                }
//            }



//        val fab: FloatingActionButton = findViewById(R.id.fab)
//        fab.setOnClickListener { view ->
//            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                .setAction("Action", null).show()
//        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_dialogues, R.id.nav_friends, R.id.nav_settings
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        navView.getHeaderView(0)
        updateAvatar()
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

        val data = mapOf("id" to mAuth!!.uid!!)
        functions!!
            .getHttpsCallable("getUser")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                Log.i("navAct", result.toString())
                navHeaderNicknameView.text = result["nickname"] as String
                navHeaderRealnameView.text = result["realname"] as String
                nickname = result["nickname"] as String
                realname = result["realname"] as String
                val url = Uri.parse(result["avatar"] as String)
                pref.putString("nickname", nickname!!)
                pref.putString("realname", realname!!)
                pref.putString("avatar", url.toString())
                Log.i("navAct", url.toString())
                if (url != null) {
                    val bitmap = PicLruCache(applicationContext)[result["avatar"] as String]
                    if (bitmap != null) {
                        navHeaderAvatarView.setImageBitmap(bitmap)
                        Log.i("navProfile", "avatar was loaded from cache")
                    } else {
                        Log.i("navProfile", "avatar will load from internet")
                        val b = Glide.with(this)
                            .asBitmap()
                            .load(url)
                            .into(object : CustomTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    navHeaderAvatarView.setImageBitmap(resource)
                                    PicLruCache(applicationContext).set(url.toString(), resource)
                                    Log.i("navProfile", "bitmap from $url is loaded and set to imageView")
                                }
                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // this is called when imageView is cleared on lifecycle call or for
                                    // some other reason.
                                    // if you are referencing the bitmap somewhere else too other than this imageView
                                    // clear it here as you can no longer have the bitmap
                                    navHeaderAvatarView.setImageBitmap(resources.getDrawable(R.drawable.defaultavatar).toBitmap())
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
            }
    }

    private fun setupPermissions() {
        val permission = ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            Log.i("SettingsActivity", "Permission to read storage denied")
            makeRequest()
        }
    }

    private fun makeRequest() {
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
    }

    private fun loadAvatarButtonOnClick() {
        val i = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        startActivityForResult(i, RESULT_LOAD_IMAGE)

        setupPermissions()
    }

    override fun onStart() {
        super.onStart()
        fun hashMapToUser(h : ArrayList<java.util.HashMap<String, Any>>) : ArrayList<User> {
            val u : ArrayList<User> = ArrayList<User>()
            h.forEach {
                u.add(User(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
            }
            return u
        }
        fun usersToStrings(h : ArrayList<User>) : ArrayList<String> {
            val s : ArrayList<String> = ArrayList<String>()
            h.forEach {
                val gson = Gson()
                val json = gson.toJson(it)
                s.add(json)
            }
            return s
        }
        val pref = SharedPreferencesUtility(applicationContext)
        val data = mapOf("id" to mAuth!!.uid!!)
        functions!!
            .getHttpsCallable("getFriends")
            .call(data).continueWith { task ->
                val result = task.result?.data as java.util.HashMap<String, Any>
                if (result["found"] as Boolean) {
                    val friends = result["friends"] as ArrayList<String>
                    val data1 = mapOf("ids" to friends)
                    functions!!
                        .getHttpsCallable("getUsers")
                        .call(data1).continueWith { task ->
                            val result1 = task.result?.data as java.util.HashMap<String, Any>
                            if (result1["found"] == true) {
                                val users = result1["users"] as ArrayList<java.util.HashMap<String, Any>>
                                val unfound = result1["unfound"] as ArrayList<String>
                                val arr1 = hashMapToUser(users)
                                val strings = usersToStrings(arr1)
                                pref.putListString("friends", strings)
                                Log.i("friendsTest", "friends was took from server")
                            }
                        }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // start Activity if granted
                    startActivityForResult(
                        Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.INTERNAL_CONTENT_URI
                        ), GET_FROM_GALLERY
                    )

                } else {
                    Log.i("SettingsActivity", " no permission to load avatar from storage")
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }
}

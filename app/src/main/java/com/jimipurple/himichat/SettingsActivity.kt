package com.jimipurple.himichat

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock.sleep
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_settings.*
import android.util.Log
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.activity_settings.dialoguesButton
import kotlinx.android.synthetic.main.activity_settings.friendsButton
import kotlinx.android.synthetic.main.activity_settings.settingsButton
import android.app.Activity
import android.provider.MediaStore
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.squareup.picasso.*
import kotlinx.io.ByteArrayOutputStream
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.jimipurple.himichat.adapters.SettingsPageAdapter
import com.jimipurple.himichat.models.*
import java.io.File
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import kotlinx.android.synthetic.main.profile_settings_fragment.*


class SettingsActivity : BaseActivity() {

    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mAuth = FirebaseAuth.getInstance()

        val data = mapOf("id" to mAuth!!.uid!!)

        val logout = {
            logoutButtonOnClick()
        }
        val loadAvatar = {
            loadAvatarButtonOnClick()
        }

        val fragmentAdapter = SettingsPageAdapter(supportFragmentManager, applicationContext, logout, loadAvatar)
        viewpager_main.adapter = fragmentAdapter
        SettingsTabs.setupWithViewPager(viewpager_main)

        //logoutButton.setOnClickListener { logoutButtonOnClick() }
        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
        //loadAvatarButton.setOnClickListener { loadAvatarButtonOnClick() }
    }

    private fun logoutButtonOnClick() {
        try {
            mAuth!!.signOut()
            sleep(100)
            val i = Intent(applicationContext, LoginActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
        } catch (e: Exception) {
            Log.i("logout:fail", e.message)
        } finally {
            Runtime.getRuntime().exit(0)
        }
    }

    private fun friendsButtonOnClick() {
        val i = Intent(applicationContext, FriendsActivity::class.java)
        startActivity(i)
    }

    private fun dialoguesButtonOnClick() {
        val i = Intent(applicationContext, DialoguesActivity::class.java)
        startActivity(i)
    }

    private fun settingsButtonOnClick() {
        val i = Intent(applicationContext, SettingsActivity::class.java)
        startActivity(i)
    }

    private fun loadAvatarButtonOnClick() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                )
            ) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            startActivityForResult(
                Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.INTERNAL_CONTENT_URI
                ), GET_FROM_GALLERY
            )
        }
        val i = Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )

        startActivityForResult(i, RESULT_LOAD_IMAGE)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = contentResolver.query(
                selectedImage!!,
                filePathColumn, null, null, null
            )
            cursor!!.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            val outputStream = ByteArrayOutputStream()
            var ref : StorageReference? = null
            val rawImage : RequestCreator?
            val setAvatar = {uri: Uri -> Unit
                val dataForJson = mapOf("id" to mAuth!!.uid, "avatar" to uri.toString())
                functions
                    .getHttpsCallable("setAvatar")
                        .call(dataForJson).addOnCompleteListener { task1 ->
                            try {
                                val i = Log.i(
                                    "setAvatar",
                                    "result " + task1.result?.data.toString()
                                )
                                val result = task1.result?.data as HashMap<String, Any>
                                val added = try {
                                    result["added"] as Boolean
                                } catch (e: Exception) {
                                    false
                                }
                                if (added) {
                                    Toast.makeText(applicationContext, resources.getText(R.string.toast_load_avatar_complete), Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(applicationContext, resources.getText(R.string.toast_load_avatar_error), Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.i("setAvatar", "error " + e.message)
                            }
                    }
            }
            Picasso.get().load(File(picturePath.trim())).resize(500, 500).centerCrop().into(object : com.squareup.picasso.Target {
                override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                    when {
                        picturePath.endsWith(".png", true) -> {
                            bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                            storage.reference.child("avatars/" + mAuth!!.uid!! + ".jpg").delete().addOnCompleteListener {
                                ref = storage.reference.child("avatars/" + mAuth!!.uid!! + ".png")
                                ref!!.putBytes(outputStream.toByteArray())
                                    .addOnSuccessListener {
                                        ref!!.downloadUrl.addOnSuccessListener { uri ->
                                            Log.i("avatar_load", "onSuccess: uri= $uri")
                                            setAvatar(uri)
                                        }
                                    }
                            }
                        }
                        picturePath.endsWith(".jpg", true) -> {
                            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            storage.reference.child("avatars/" + mAuth!!.uid!! + ".png").delete().addOnCompleteListener {
                                ref = storage.reference.child("avatars/" + mAuth!!.uid!! + ".jpg")
                                ref!!.putBytes(outputStream.toByteArray())
                                    .addOnSuccessListener {
                                        ref!!.downloadUrl.addOnSuccessListener { uri ->
                                            Log.i("avatar_load", "onSuccess: uri= $uri")
                                            setAvatar(uri)
                                        }
                                    }
                            }
                        }
                        picturePath.endsWith(".jpeg", true) -> {
                            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            storage.reference.child("avatars/" + mAuth!!.uid!! + ".png").delete().addOnCompleteListener {
                                ref = storage.reference.child("avatars/" + mAuth!!.uid!! + ".jpg")
                                ref!!.putBytes(outputStream.toByteArray())
                                    .addOnSuccessListener {
                                        ref!!.downloadUrl.addOnSuccessListener { uri ->
                                            Log.i("avatar_load", "onSuccess: uri= $uri")
                                            setAvatar(uri)
                                        }
                                    }
                            }
                        }
                    }

                }

                override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                    Log.i("avatar_load", "Загрузка изображения не удалась " + picturePath + "\n" + e?.message)
                }
            })
        }


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

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("POSITION", SettingsTabs.getSelectedTabPosition())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        viewpager_main.currentItem = savedInstanceState.getInt("POSITION")
    }
}

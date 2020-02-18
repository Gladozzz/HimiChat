package com.jimipurple.himichat.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.requestPermissions
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.jimipurple.himichat.*
import com.jimipurple.himichat.adapters.SettingsPageAdapter
import com.squareup.picasso.RequestCreator
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.android.synthetic.main.fragment_settings.viewpager_main
import kotlinx.io.ByteArrayOutputStream
import java.io.File


class SettingsFragment : BaseFragment() {

    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var fragmentAdapter: SettingsPageAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        val data = mapOf("id" to mAuth!!.uid!!)

        val logout = {
            logoutButtonOnClick()
        }
        val loadAvatar = {
            loadAvatarButtonOnClick()
        }

        fragmentAdapter = SettingsPageAdapter(this.childFragmentManager, c!!.applicationContext, logout, loadAvatar)
        viewpager_main.adapter = fragmentAdapter
        SettingsTabs.setupWithViewPager(viewpager_main)
    }

    private fun logoutButtonOnClick() {
        try {
            mAuth!!.signOut()
            SystemClock.sleep(100)
            val i = Intent(c!!.applicationContext, LoginActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(i)
        } catch (e: Exception) {
            Log.i("logout:fail", e.message)
        } finally {
            Runtime.getRuntime().exit(0)
        }
    }

    private fun loadAvatarButtonOnClick() {
        if (checkPermission()) {
            val i = Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            )
            startActivityForResult(i, RESULT_LOAD_IMAGE)
        }
    }

    private fun checkPermission(): Boolean {
        return if (ActivityCompat.checkSelfPermission(c!!, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(activity!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
            false
        } else {
            Log.e("settingsFragment", "PERMISSION GRANTED")
            true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
            val selectedImage = data.data
            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)

            val cursor = c!!.contentResolver.query(
                selectedImage!!,
                filePathColumn, null, null, null
            )
            cursor!!.moveToFirst()

            val columnIndex = cursor.getColumnIndex(filePathColumn[0])
            val picturePath = cursor.getString(columnIndex)
            cursor.close()
            val outputStream = ByteArrayOutputStream()
            var ref: StorageReference?
            val rawImage : RequestCreator?
            val setAvatar = {uri: Uri -> Unit
                val dataForJson = mapOf("id" to mAuth!!.uid, "avatar" to uri.toString())
                functions!!
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
                                Toast.makeText(c!!.applicationContext, resources.getText(R.string.toast_load_avatar_complete), Toast.LENGTH_SHORT).show()
                                val f = fragmentAdapter!!.getCurrentFragment()!!
                                if (f is ProfileSettingsFragment) {
                                    f.updateAvatar()
                                }
                                val a = activity!!
                                if (a is NavigationActivity) {
                                    a.updateAvatar()
                                }
                            } else {
                                Toast.makeText(c!!.applicationContext, resources.getText(R.string.toast_load_avatar_error), Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Log.i("setAvatar", "error " + e.message)
                        }
                    }
            }
            Glide.with(this)
                .asBitmap()
                .load(File(picturePath.trim()))
                .override(500, 500)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                        when {
                            picturePath.endsWith(".png", true) -> {
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
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
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
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
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
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
                    override fun onLoadCleared(placeholder: Drawable?) {
                        // this is called when imageView is cleared on lifecycle call or for
                        // some other reason.
                        // if you are referencing the bitmap somewhere else too other than this imageView
                        // clear it here as you can no longer have the bitmap
                    }

                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        super.onLoadFailed(errorDrawable)
                        Log.e("avatar_load", "Загрузка изображения не удалась $picturePath")
                    }
                })
        }
    }
}

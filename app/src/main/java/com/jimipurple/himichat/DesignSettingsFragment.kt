package com.jimipurple.himichat


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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import com.squareup.picasso.RequestCreator
import java.io.File
import kotlinx.android.synthetic.main.activity_settings.dialoguesButton
import kotlinx.android.synthetic.main.activity_settings.friendsButton
import kotlinx.android.synthetic.main.activity_settings.settingsButton
import kotlinx.android.synthetic.main.profile_settings_fragment.*
import kotlinx.io.ByteArrayOutputStream


class DesignSettingsFragment : Fragment() {

    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()
    private var nickname: String? = null
    private var realname: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        val data = mapOf("id" to mAuth!!.uid!!)
        functions
            .getHttpsCallable("getUser")
            .call(data).continueWith { task ->
                val result = task.result?.data as HashMap<String, Any>
                Log.i("settings", result.toString())
                nicknameEdit.setText(result["nickname"] as String)
                realnameEdit.setText(result["realname"] as String)
                nickname = result["nickname"] as String
                realname = result["realname"] as String
                Picasso.get().load(result["avatar"] as String).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        avatarView.setImageBitmap(bitmap)
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        Log.i("avatar", "Загрузка изображения не удалась " + result["avatar"] as String + "\n" + e?.message)
                    }
                })
            }

        //logoutButton.setOnClickListener { logoutButtonOnClick() }
        //loadAvatarButton.setOnClickListener { loadAvatarButtonOnClick() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.design_settings_fragment, container, false)
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
}

package com.jimipurple.himichat.ui.settings


import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import com.jimipurple.himichat.R
import com.jimipurple.himichat.db.MessagesDBHelper
import com.squareup.picasso.LruCache
import kotlinx.android.synthetic.main.profile_settings_fragment.*
import java.util.regex.Pattern


class ProfileSettingsFragment(val logoutCallback: () -> Unit, val loadAvatarCallback: () -> Unit) : Fragment() {

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
    private val ARG_PAGE = "ARG_PAGE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        loadAvatarButton.setOnClickListener { loadAvatarCallback() }
        deleteAllMessagesButton.setOnClickListener { deleteAllMessages() }

        updateAvatar()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile_settings_fragment, container, false)
    }
    fun updateAvatar() {
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
                renameNicknameButton.setOnClickListener { loadUserData() }
                renameRealnameButton.setOnClickListener { loadUserData() }
                logoutButton.setOnClickListener { logoutCallback() }
                val url = Uri.parse(result["avatar"] as String)
                if (url != null) {
                    val bitmap = LruCache(context!!)[result["avatar"] as String]
                    if (bitmap != null) {
                        avatarView.setImageBitmap(bitmap)
                    } else {
                        Glide.with(this)
                            .asBitmap()
                            .load(url)
                            .into(object : CustomTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    avatarView.setImageBitmap(resource)
                                    LruCache(context!!).set(url.toString(), resource)
                                    Log.i("Profile", "bitmap from $url is loaded and set to imageView")
                                }
                                override fun onLoadCleared(placeholder: Drawable?) {
                                    // this is called when imageView is cleared on lifecycle call or for
                                    // some other reason.
                                    // if you are referencing the bitmap somewhere else too other than this imageView
                                    // clear it here as you can no longer have the bitmap
//                                    avatarView.setImageBitmap(resources.getDrawable(R.drawable.defaultavatar).toBitmap())
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    super.onLoadFailed(errorDrawable)
                                    Log.e("Profile", "Загрузка изображения не удалась $url")
                                }
                            })
                    }
                } else {
                    Log.i("Profile", "avatar wasn't received")
                }
            }
    }

//    fun newInstance(page: Int): ProfileSettingsFragment {
//        val args = Bundle()
//        args.putInt(ARG_PAGE, page)
//        val fragment = ProfileSettingsFragment()
//        fragment.arguments = args
//        return fragment
//    }

    fun deleteAllMessages() {
        MessagesDBHelper(context!!).deleteAllMessages()
    }

    fun isNicknameValid(nickname: String): Boolean {
        val expression  = "^[a-z0-9_-]{4,15}\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
    }

    private fun loadUserData() {
        if (isNicknameValid(nicknameEdit.text.toString()) && realnameEdit.text.toString() != ""){
            nickname = nicknameEdit.text.toString()
            realname = realnameEdit.text.toString()
            val data = mapOf("id" to mAuth!!.uid!!, "nickname" to nickname, "realname" to realname)
            functions
                .getHttpsCallable("getUser")
                .call(data).continueWith { task ->
                    //
                }
        } else {
            nicknameEdit.setText(nickname)
            realnameEdit.setText(realname)
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
}

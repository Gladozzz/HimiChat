package com.jimipurple.himichat.ui.settings


import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.NavigationActivity
import com.jimipurple.himichat.R
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import com.squareup.picasso.LruCache
import kotlinx.android.synthetic.main.profile_settings_fragment.*
import java.util.regex.Pattern


class ProfileSettingsFragment : BaseFragment() {

    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3

    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var nickname: String? = null
    private var realname: String? = null
    private var avatar: String? = null
    private val ARG_PAGE = "ARG_PAGE"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
//        loadAvatarButton.setOnClickListener { loadAvatarCallback() }
        avatarView.setOnClickListener { app!!.loadAvatarCallback() }
        deleteAllMessagesButton.setOnClickListener { deleteAllMessages() }
        nicknameEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                nicknameEdit.setTextColor(resources.getColor(R.color.white))
//                cancelNicknameButton.visibility = View.VISIBLE
            }
        })
        realnameEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                realnameEdit.setTextColor(resources.getColor(R.color.white))
//                cancelRealnameButton.visibility = View.VISIBLE
            }
        })
        renameNicknameButton.setOnClickListener { setNickname() }
        renameRealnameButton.setOnClickListener { setRealname() }
        cancelNicknameButton.setOnClickListener { updateNickname() }
        cancelRealnameButton.setOnClickListener { updateRealname() }
        logoutButton.setOnClickListener { app!!.logoutCallback() }

        uptadeBySaved()
        updateAvatar()
        updateNickname()
        updateRealname()
    }

    private fun updateNickname() {
        firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                nickname = userData.get("nickname") as String?
                if (nickname == null) {
                    nickname = ""
                }
                SharedPreferencesUtility(c!!).putString("nickname", nickname!!)
                nicknameEdit.setText(nickname)
                nicknameEdit.setTextColor(resources.getColor(R.color.white))
                cancelNicknameButton.visibility = View.GONE
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }

    private fun updateRealname() {
        firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                realname = userData.get("real_name") as String?
                if (realname == null) {
                    realname = ""
                }
                SharedPreferencesUtility(c!!).putString("realname", realname!!)
                realnameEdit.setText(realname)
                realnameEdit.setTextColor(resources.getColor(R.color.white))
                cancelRealnameButton.visibility = View.GONE
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile_settings_fragment, container, false)
    }
    fun uptadeBySaved() {
        val pref = SharedPreferencesUtility(c!!)
        val savedNickname = pref.getString("nickname")
        val savedRealname = pref.getString("realname")
        var savedAvatar = pref.getString("avatar")
        nicknameEdit.setText(savedNickname)
        realnameEdit.setText(savedRealname)
        if (savedAvatar != null) {
            val bitmap = LruCache(c!!)[savedAvatar]
            if (bitmap != null) {
                avatarView.setImageBitmap(bitmap)
                Log.i("ProfileSettings", "avatar was loaded from cache")
            } else {
                Log.e("ProfileSettings", "Avatar is not in cache. Preload can be done")
            }
        } else {
            Log.e("ProfileSettings", "avatar was not in SharedPrefences")
        }
    }
    fun updateAvatar() {
        firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                avatar = userData.get("avatar") as String?
                if (avatar == null) {
                    avatar = ""
                }
                Glide.with(this)
                    .asBitmap()
                    .load(avatar)
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            avatarView.setImageBitmap(resource)
                            LruCache(context!!).set(avatar!!, resource)
                            SharedPreferencesUtility(c!!).putString("avatar", avatar!!)
                            Log.i("Profile", "bitmap from $avatar is loaded and set to imageView")
                        }
                        override fun onLoadCleared(placeholder: Drawable?) {
                            // this is called when imageView is cleared on lifecycle call or for
                            // some other reason.
                            // if you are referencing the bitmap somewhere else too other than this imageView
                            // clear it here as you can no longer have the bitmap
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            Log.e("Profile", "Загрузка изображения не удалась $avatar")
                        }
                    })
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }

    private fun setNickname() {
        if (isNicknameValid(nicknameEdit.text.toString())){
            nickname = nicknameEdit.text.toString()
            firestore!!.collection("users").document(mAuth!!.uid!!).update(mapOf("nickname" to  nickname)).addOnSuccessListener { (app!!.currentActivity as NavigationActivity).updateAvatar() }
            cancelNicknameButton.visibility = View.GONE
        } else {
            nicknameEdit.setTextColor(resources.getColor(R.color.red))
//            firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
//                if (it.isSuccessful) {
//                    val userData = it.result!!
//                    nickname = userData.get("nickname") as String?
//                    if (nickname == null) {
//                        nickname = ""
//                    }
//                    nicknameEdit.setText(nickname)
//                } else {
//                    Log.i("FirestoreRequest", "Error getting documents.", it.exception)
//                }
//            }
        }
    }

    private fun setRealname() {
        if (realnameEdit.text.isNotEmpty()){
            realname = realnameEdit.text.toString()
            firestore!!.collection("users").document(mAuth!!.uid!!).update(mapOf("nickname" to  realname))
            cancelRealnameButton.visibility = View.GONE
        } else {
            realnameEdit.setTextColor(resources.getColor(R.color.red))
//            firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
//                if (it.isSuccessful) {
//                    val userData = it.result!!
//                    realname = userData.get("nickname") as String?
//                    if (realname == null) {
//                        realname = ""
//                    }
//                    realnameEdit.setText(realname)
//                } else {
//                    Log.i("FirestoreRequest", "Error getting documents.", it.exception)
//                }
//            }
        }
    }

    fun deleteAllMessages() {
        MessagesDBHelper(c!!).deleteAllMessages()
    }

    fun isNicknameValid(nickname: String): Boolean {
        val expression  = "^[^0-9][^@#\$%^%&*_()]{3,15}+\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
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

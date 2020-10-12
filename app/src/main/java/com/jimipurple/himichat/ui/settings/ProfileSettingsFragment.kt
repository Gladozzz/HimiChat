package com.jimipurple.himichat.ui.settings


import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.app.ActivityCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.NavigationActivity
import com.jimipurple.himichat.R
import com.jimipurple.himichat.data.FirebaseSource
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.utills.SharedPreferencesUtility
import com.squareup.picasso.LruCache
import com.squareup.picasso.RequestCreator
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.profile_settings_fragment.*
import kotlinx.android.synthetic.main.profile_settings_fragment.nicknameEdit
import kotlinx.android.synthetic.main.profile_settings_fragment.nicknameInput
import kotlinx.android.synthetic.main.profile_settings_fragment.realnameEdit
import net.sectorsieteg.avatars.AvatarDrawableFactory
import java.io.ByteArrayOutputStream
import java.io.File


class ProfileSettingsFragment : BaseFragment() {

    private val RESULT_LOAD_IMAGE = 1
    private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2
    private val GET_FROM_GALLERY = 3

    private var storage: FirebaseStorage = FirebaseStorage.getInstance()
    private var nickname: String? = null
    private var realname: String? = null
    private var avatar: String? = null
    private val ARG_PAGE = "ARG_PAGE"
    private lateinit var profileSettingsViewModel: ProfileSettingsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        avatarView.setOnClickListener {
            val a = requireActivity()
            if (a is NavigationActivity) {
//                a.loadAvatarButtonOnClick()
                loadAvatarButtonOnClick()
            }
        }
        logoutButton.setOnClickListener {
            FirebaseSource(c!!).logout()
        }
        saveAccountButton.setOnClickListener {
            saveAccountData()
        }
        deleteAllMessagesButton.setOnClickListener { deleteAllMessages() }

        updateDataFromServer()
        updateAvatar()

        profileSettingsViewModel = ViewModelProviders.of(this)
            .get(ProfileSettingsViewModel::class.java)

        //set up model for profile settings form
        profileSettingsViewModel.profileSettingsFormState.observe(viewLifecycleOwner, Observer {
            val profileSettingsState = it ?: return@Observer

            // disable login button unless both username / password is valid
            saveAccountButton.isEnabled = profileSettingsState.isDataValid
            saveAccountButton.isVisible = !profileSettingsState.isDataAnalyzing
            settingsProgressBar.isVisible = profileSettingsState.isDataAnalyzing

            nicknameInput.isErrorEnabled = false
            realNameInput.isErrorEnabled = false
            if (profileSettingsState.nicknameError != null) {
                nicknameInput.error = getString(profileSettingsState.nicknameError!!)
                nicknameInput.isErrorEnabled = true
            }
            if (profileSettingsState.realNameError != null) {
                realNameInput.error = getString(profileSettingsState.realNameError!!)
                realNameInput.isErrorEnabled = true
            }
        })
        nicknameEdit.apply {
            afterTextChanged {
                profileSettingsViewModel.accountDataChanged(
                    nicknameEdit.text.toString(),
                    realnameEdit.text.toString()
                )
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        saveAccountData()
                }
                false
            }
        }
        realnameEdit.apply {
            afterTextChanged {
                profileSettingsViewModel.accountDataChanged(
                    nicknameEdit.text.toString(),
                    realnameEdit.text.toString()
                )
            }
            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        saveAccountData()
                }
                false
            }
        }
    }

    /**
     * Extension function to simplify setting an afterTextChanged action to EditText components.
     */
    fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadAvatarButtonOnClick() {
        val a = requireActivity()
        if (a is NavigationActivity) {
            if (ActivityCompat.checkSelfPermission(a, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    app!!.currentActivity!!,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
                )
            } else {
                Log.i(tag, "load PERMISSION GRANTED")
                val i = Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                )
                this.startActivityForResult(i, RESULT_LOAD_IMAGE)
            }
        }
    }

    private fun updateDataFromServer() {
        fbSource!!.getUser(mAuth!!.uid!!, { user ->
            if (nicknameEdit!= null) {
                nickname = user.nickname
                realname = user.realName
                avatar = user.avatar
                profileSettingsViewModel.currentNicknameOnServer = nickname
                profileSettingsViewModel.currentRealnameOnServer = realname
                nicknameEdit.setText(user.nickname)
                realnameEdit.setText(user.realName)
                val bitmap = LruCache(c!!)[user.avatar]
                if (bitmap != null) {
                    avatarView.setImageBitmap(bitmap)
                    Log.i("ProfileSettings", "avatar was loaded from cache")
                } else {
                    Log.e("ProfileSettings", "Avatar is not in cache. Preload can be done")
                }
            }
        })
    }

    private fun saveAccountData() {
        nickname = nicknameEdit.text.toString()
        realname = realnameEdit.text.toString()
        val accountDoc = firestore!!.collection("users").document(mAuth!!.uid!!)
        accountDoc.update(mapOf("nickname" to  nickname))
        accountDoc.update(mapOf("real_name" to  realname))
        var checkDataSaving = true
        accountDoc.get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                val r = userData.get("real_name") as String?
                val n = userData.get("nickname") as String?
                if (r != realname) {
                    checkDataSaving = false
                }
                if (n != nickname) {
                    checkDataSaving = false
                }
                if (checkDataSaving) {
                    Toast.makeText(c, R.string.toast_saving_account_success, Toast.LENGTH_SHORT).show()
                    saveAccountButton.isEnabled = false
                } else {
                    Toast.makeText(c, R.string.toast_saving_account_error, Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }

    @ColorInt
    fun getColorFromAttr(): Int {
        val typedValue = TypedValue()
        currentTheme!!.resolveAttribute(R.attr.primaryTextColor, typedValue, true)
        return typedValue.data
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.profile_settings_fragment, container, false)
    }

    fun updateAvatar() {
        fbSource!!.getUser(mAuth!!.uid!!, { user ->
            avatar = user.avatar
            val bitmap = LruCache(c!!.applicationContext)[avatar!!]
            if (bitmap != null) {
                avatarView.setImageBitmap(bitmap)
            }
            Glide.with(c!!)
                .asBitmap()
                .load(avatar)
                .into(object : CustomTarget<Bitmap>(){
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        try {
                            avatarView.setImageBitmap(resource)
                            LruCache(c!!).set(avatar!!, resource)
                            Log.i("Profile", "bitmap from $avatar is loaded and set to imageView")
                        } catch (e: Exception) {
                            Log.e("Profile", "e " + e.message)
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
                        Log.e("Profile", "Загрузка изображения не удалась $avatar")
                    }
                })
        })
    }

    private fun deleteAllMessages() {
        MessagesDBHelper(c!!).deleteAllMessages()
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
//                    this.startActivityForResult(
//                        Intent(
//                            Intent.ACTION_PICK,
//                            MediaStore.Images.Media.INTERNAL_CONTENT_URI
//                        ), GET_FROM_GALLERY
//                    )
                    val i = Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                    this.startActivityForResult(i, RESULT_LOAD_IMAGE)

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (nicknameEdit != null) {
            outState.putString("nickname", nicknameEdit.text.toString())
        }
        if (realnameEdit != null) {
            outState.putString("realName", realnameEdit.text.toString())
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null) {
            nicknameEdit.setText(savedInstanceState.getString("nickname"))
            realnameEdit.setText(savedInstanceState.getString("realName"))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.i(tag, "onActivityResult $requestCode $resultCode $data")

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && null != data) {
//        if (requestCode == RESULT_LOAD_IMAGE) {
            Toast.makeText(c!!, R.string.toast_load_avatar_warning, Toast.LENGTH_LONG).show()
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
                firestore!!.collection("users").document(mAuth!!.uid!!).update("avatar", uri.toString()).addOnCompleteListener { task1 ->
                    try {
                        val i = Log.i(
                            "setAvatar",
                            "result " + task1.result?.toString()
                        )
                        if (task1.isSuccessful) {
                            Toast.makeText(c!!.applicationContext, resources.getText(R.string.toast_load_avatar_complete), Toast.LENGTH_SHORT).show()
                            updateAvatar()
                            val a = requireActivity()
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
                                                Toast.makeText(requireContext(), R.string.toast_load_avatar_complete, Toast.LENGTH_LONG).show()
                                                requireActivity().recreate()
                                                setAvatar(uri)
                                            }
                                        }.addOnFailureListener {
                                            Log.i("avatar_load", "onFailure: error= ${it.message}")
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
                                                Toast.makeText(requireContext(), R.string.toast_load_avatar_complete, Toast.LENGTH_LONG).show()
                                                setAvatar(uri)
                                            }
                                        }.addOnFailureListener {
                                            Log.i("avatar_load", "onFailure: error= ${it.message}")
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
                                                Toast.makeText(requireContext(), R.string.toast_load_avatar_complete, Toast.LENGTH_LONG).show()
                                                setAvatar(uri)
                                            }
                                        }.addOnFailureListener {
                                            Log.i("avatar_load", "onFailure: error= ${it.message}")
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
                        Toast.makeText(requireContext(), R.string.toast_load_avatar_error, Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
}

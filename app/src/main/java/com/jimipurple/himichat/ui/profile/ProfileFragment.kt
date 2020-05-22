package com.jimipurple.himichat.ui.profile

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.User
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*


class ProfileFragment : BaseFragment() {

    val REQUEST_CODE_DIALOG_ACTIVITY = 1
    var profile_id : String? = null
    private var nickname: String? = null
    private var realname: String? = null
    private var avatar: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        mAuth = FirebaseAuth.getInstance()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profile_id = requireArguments()["profile_id"] as String
        if (profile_id == mAuth!!.uid!!) {
            setOwnProfileMode()
        } else {
            setAnotherProfileMode()
        }
        profileSendMessageButton.setOnClickListener {
            val b = Bundle()
            b.putString("friend_id", profile_id)
            b.putString("nickname", nickname)
            b.putString("avatar", avatar)
            val navController = findNavController()
            navController.navigate(R.id.nav_dialog, b)
        }
        profileRemoveFriendButton.setOnClickListener {
//            val data = mapOf("ids" to ids)
//            functions!!
//                .getHttpsCallable("getUsers")
//                .call(data).continueWith { task ->
//                    val result = task.result?.data as HashMap<String, Any>
//                    Log.i("dialogsAct", result.toString())
//                    if (result["found"] == true) {
//
//                    }
//                }
        }
        firestore!!.collection("users").document(profile_id!!).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val userData = it.result!!
                nickname = userData.get("nickname") as String?
                if (nickname == null) {
                    nickname = ""
                }
                realname = userData.get("real_name") as String?
                if (realname == null) {
                    realname = ""
                }
                avatar = userData.get("avatar") as String?
                if (avatar == null) {
                    avatar = ""
                }
                val user = User(profile_id!!, nickname!!, realname!!, avatar!!)

                Glide.with(this)
                    .asBitmap()
                    .load(avatar)
                    .into(object : CustomTarget<Bitmap>(){
                        override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                            if (profileAvatarView != null) {
                                profileAvatarView.setImageBitmap(bitmap)
                            }
                            LruCache(c!!).set(avatar!!, bitmap)
                            Log.i("Profile", "bitmap from $avatar is loaded and set to imageView")
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
                            Log.e("Profile", "Загрузка изображения не удалась $avatar")
                        }
                    })
                nicknameEdit.setText(nickname)
                realnameEdit.setText(realname)
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }

    private fun setOwnProfileMode() {
        profileRemoveFriendButton.visibility = View.GONE
        profileSendMessageButton.visibility = View.GONE

        profileEditButton.visibility = View.VISIBLE
    }

    private fun setAnotherProfileMode() {
        profileSendMessageButton.visibility = View.VISIBLE
        profileRemoveFriendButton.visibility = View.VISIBLE

        profileEditButton.visibility = View.GONE
    }
}

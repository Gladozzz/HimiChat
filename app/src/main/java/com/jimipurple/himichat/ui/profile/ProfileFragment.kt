package com.jimipurple.himichat.ui.profile

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.firestore.FieldValue
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.NavigationActivity
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.User
import com.squareup.picasso.LruCache
import kotlinx.android.synthetic.main.fragment_profile.*
import kotlinx.android.synthetic.main.fragment_profile.nicknameEdit
import kotlinx.android.synthetic.main.fragment_profile.profileEditButton


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
        profile_id = requireArguments()["profile_id"] as String
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        profileEditButton.setOnClickListener {
            val b = Bundle()
            val navController = findNavController()
            navController.navigate(R.id.nav_settings, b)
        }
        profileRemoveFriendButton.setOnClickListener {
            val data = mapOf("id" to mAuth!!.uid!!, "friendId" to profile_id!!)
//            functions!!
//                .getHttpsCallable("removeFriend")
//                .call(data).continueWith { task ->
//                    val result = task.result?.data as HashMap<String, Any>
//                    Log.i("removeFriend", "result $result")
//                    if (task.exception != null) {
//                        Log.e("removeFriend", "exception " + task.exception.toString())
//                    }
//                }.addOnSuccessListener {
//                    Toast.makeText(c!!, R.string.toast_remove_friend_complete, Toast.LENGTH_LONG).show()
//                    requireActivity().recreate()
//                }.addOnFailureListener {
//                    Log.e("removeFriend", "error " + it.message + " " + it.cause)
//                    Toast.makeText(c!!, R.string.toast_remove_friend_error, Toast.LENGTH_LONG).show()
//                }
            val uid = mAuth!!.uid!!
            //removing all existing invites of between those users
            firestore!!.collection("users").document(uid).update(mapOf("invited_by" to  FieldValue.arrayRemove(profile_id!!)))
            firestore!!.collection("users").document(profile_id!!).update(mapOf("invites" to  FieldValue.arrayRemove(uid)))
            firestore!!.collection("users").document(uid).update(mapOf("invites" to  FieldValue.arrayRemove(profile_id!!)))
            firestore!!.collection("users").document(profile_id!!).update(mapOf("invited_by" to  FieldValue.arrayRemove(uid)))
            //removing from friends list's
            firestore!!.collection("users").document(profile_id!!).update(mapOf("friends" to  FieldValue.arrayRemove(uid)))
            firestore!!.collection("users").document(uid).update(mapOf("friends" to  FieldValue.arrayRemove(profile_id!!)))
            Toast.makeText(c!!, R.string.toast_remove_friend_complete, Toast.LENGTH_LONG).show()
            requireActivity().recreate()
        }
        firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
            if (it.isSuccessful) {
                val data = it.result!!
                val friendsIDs = data.get("friends") as? ArrayList<String>?
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
                        if (friendsIDs != null) {
                            for (i in friendsIDs) {
                                if (profile_id == i) {
                                    setFriendProfileMode()
                                }
                            }
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
            } else {
                Log.e("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }

    private fun setOwnProfileMode() {
        if (profileRemoveFriendButton != null) {
            profileRemoveFriendButton.visibility = View.GONE
        }
        if (profileSendMessageButton != null) {
            profileSendMessageButton.visibility = View.GONE
        }
        if (profileInviteButton != null) {
            profileInviteButton.visibility = View.GONE
        }

        if (profileEditButton != null) {
            profileEditButton.visibility = View.VISIBLE
        }
    }

    private fun setAnotherProfileMode() {
        if (profileSendMessageButton != null) {
            profileSendMessageButton.visibility = View.GONE
        }
        if (profileRemoveFriendButton != null) {
            profileRemoveFriendButton.visibility = View.GONE
        }
        if (profileEditButton != null) {
            profileEditButton.visibility = View.GONE
        }

        if (profileInviteButton != null) {
            profileInviteButton.visibility = View.VISIBLE
        }
    }

    private fun setFriendProfileMode() {
        if (profileSendMessageButton != null) {
            profileSendMessageButton.visibility = View.VISIBLE
        }
        if (profileRemoveFriendButton != null) {
            profileRemoveFriendButton.visibility = View.VISIBLE
        }

        if (profileEditButton != null) {
            profileEditButton.visibility = View.GONE
        }
        if (profileInviteButton != null) {
            profileInviteButton.visibility = View.GONE
        }
    }
}

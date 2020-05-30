package com.jimipurple.himichat.ui.friends

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.jimipurple.himichat.*
import com.squareup.picasso.LruCache
import kotlinx.android.synthetic.main.fragment_find_friend.*
import kotlinx.serialization.UnstableDefault
import java.util.regex.Pattern


class FindFriendFragment : BaseFragment() {

    var foundId = "" // id найденного пользователя

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_find_friend, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        nicknameEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                makeUnvisibleFound()
                findfriendSendMessageButton.visibility = View.GONE
                findfriendRemoveFriendButton.visibility = View.GONE
                findfriendInviteButton.visibility = View.VISIBLE
                findButtonOnClick()
//                cancelRealnameButton.visibility = View.VISIBLE
            }
        })
//        findButton.setOnClickListener { findButtonOnClick() }
        findfriendInviteButton.setOnClickListener { inviteButtonOnClick() }
    }

    @UnstableDefault
    private fun findButtonOnClick() {
        if (nicknameEdit.text.isNotEmpty()) {
            var nickname = nicknameEdit.text.toString()
            if (isNicknameValid(nickname)) {
                var data = hashMapOf(
                    "nickname" to nickname
                )
                var id = ""
                var res = functions!!
                    .getHttpsCallable("findUser")
                    .call(data).addOnCompleteListener { task ->
                        try {
                            Log.i("findUser:find", "result " + task.result?.data.toString())
                            var result = task.result?.data as HashMap<String, Any>
                            if (result["found"] as Boolean) {
                                id = result["id"] as String
                                foundId = id
                                Log.i("findUser:find", "id $id")
                                data = hashMapOf(
                                    "id" to id
                                )
                                firestore!!.collection("users").document(mAuth!!.uid!!).get().addOnCompleteListener{
                                    if (it.isSuccessful) {
                                        val userData = it.result!!
                                        val friendsIDs = userData.get("friends") as? ArrayList<String>?
                                        firestore!!.collection("users").document(id).get().addOnCompleteListener{
                                            if (it.isSuccessful) {
                                                findfriendSendMessageButton.visibility = View.GONE
                                                findfriendRemoveFriendButton.visibility = View.GONE
                                                findfriendInviteButton.visibility = View.VISIBLE
                                                val userData = it.result!!
                                                Log.i("findUser:get", "result $userData")
//                                                id = userData["id"] as String
                                                try {
                                                    nickname = userData["nickname"] as String
                                                } catch (e: Exception) {
                                                    nickname = ""
                                                    Log.i("findUser:get", e.message)
                                                }
                                                var rn = ""
                                                try {
                                                    rn = userData["real_name"] as String
                                                } catch (e: Exception) {
                                                    rn = ""
                                                    Log.i("findUser:get", e.message)
                                                }
                                                val avatar = try {
                                                    userData["avatar"] as String
                                                } catch (e: Exception) {
                                                    null
                                                }
                                                if (avatar != null) {
                                                    val bitmap = LruCache(c!!)[avatar]
                                                    if (bitmap != null) {
                                                        findfriendAvatarView.setImageBitmap(bitmap)
                                                    } else {
                                                        Glide.with(this)
                                                            .asBitmap()
                                                            .load(avatar)
                                                            .into(object : CustomTarget<Bitmap>(){
                                                                override fun onResourceReady(bitmap: Bitmap, transition: Transition<in Bitmap>?) {
                                                                    findfriendAvatarView.setImageBitmap(bitmap)
                                                                    findfriendAvatarView.setOnClickListener {
                                                                        val b = Bundle()
                                                                        b.putString("profile_id", id)
                                                                        val navController = findNavController()
                                                                        navController.navigate(R.id.nav_profile, b)
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
                                                    }
                                                } else {
                                                    Log.i("findUser", "avatar wasn't received")
                                                }
                                                if (friendsIDs != null) {
                                                    for (i in friendsIDs) {
                                                        if (foundId == i) {
                                                            findfriendSendMessageButton.visibility = View.VISIBLE
                                                            findfriendRemoveFriendButton.visibility = View.VISIBLE
                                                            findfriendInviteButton.visibility = View.GONE
                                                            findfriendSendMessageButton.setOnClickListener {
                                                                val b = Bundle()
                                                                b.putString("friend_id", foundId)
                                                                b.putString("nickname", nickname)
                                                                b.putString("avatar", avatar)
                                                                val navController = findNavController()
                                                                navController.navigate(R.id.nav_dialog, b)
                                                            }
                                                        }
                                                    }
                                                }
                                                if (foundId == mAuth!!.uid!!) {
                                                    findfriendSendMessageButton.visibility = View.GONE
                                                    findfriendRemoveFriendButton.visibility = View.GONE
                                                    findfriendInviteButton.visibility = View.GONE
                                                }
                                                makeVisibleFound()
                                                foundNickname.setText(nickname)
                                                findfriendRealNameEdit.setText(rn)
                                                Log.i("findUser:get", "id $id")
                                                Log.i("findUser:get", "nickname $nickname")
                                                Log.i("findUser:get", "real name $rn")
                                                Log.i("findUser:get", "avatar $avatar")
                                                Log.i("findUser:get", "friendsIDs $friendsIDs")
                                            } else {
                                                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
                                            }
                                        }
                                    } else {
                                        Log.e("FirestoreRequest", "Error getting documents.", it.exception)
                                    }
                                }
                            } else {
                                Log.i("findUser:find", "Пользователь с ником $nickname не найден")
//                                Toast.makeText(c!!, R.string.toast_user_nickname_not_found, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.i("findUser:find", "error " + e.message)
                        }
                        //messageInput.setText("")
                    }
            } else {
//                Toast.makeText(c!!, R.string.toast_nickname_not_valid, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun inviteButtonOnClick() {
        when {
            foundId.isNotEmpty() -> {
                //val data = mapOf("invites" to arrayOf(mAuth!!.uid))
                val uid = mAuth!!.currentUser!!.uid
                val data = mapOf("inviterId" to uid, "id" to foundId)
                Log.i("inviteUser", "id of inviting user $foundId")
                Log.i("inviteUser", "uid $uid")
                //firestore.collection("users").document(foundId).set(data, SetOptions.merge())
                functions!!
                    .getHttpsCallable("inviteUser")
                    .call(data)
                    .continueWith { task ->
                        // This continuation runs on either success or failure, but if the task
                        // has failed then result will throw an Exception which will be
                        // propagated down.
                        val result = task.result?.data as HashMap<String, Any>
                        if (result["invite"] == true) {
                            Toast.makeText(c!!, R.string.toast_invite_successful, Toast.LENGTH_LONG).show()
                        } else if (result["invite"] == false) {
                            if (result["reason"] == "already invited") {
                                Toast.makeText(c!!, R.string.toast_invite_already, Toast.LENGTH_LONG).show()
                            } else if (result["reason"] == "already invited you") {
                                Toast.makeText(c!!, R.string.toast_invite_already_you, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
            mAuth!!.uid == foundId -> {
                Toast.makeText(c!!, R.string.toast_selfinvite, Toast.LENGTH_LONG).show()
                Log.i("inviteUser::failure", "foundId == uid")
            }
            else -> {
                Toast.makeText(c!!, R.string.toast_no_found_user, Toast.LENGTH_LONG).show()
                Log.i("inviteUser::failure", "foundId is empty")
            }
        }
    }

    fun isNicknameValid(nickname: String): Boolean {
        val expression  = "^[a-z0-9_-]{4,15}\$"
        val pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(nickname)
        return matcher.matches()
    }

    fun makeUnvisibleFound() {
        found.visibility = View.GONE
    }

    fun makeVisibleFound() {
        found.visibility = View.VISIBLE
    }
}

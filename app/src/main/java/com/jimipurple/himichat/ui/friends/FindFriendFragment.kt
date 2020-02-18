package com.jimipurple.himichat.ui.friends

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.jimipurple.himichat.*
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
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
        findButton.setOnClickListener { findButtonOnClick() }
        inviteButton.setOnClickListener { inviteButtonOnClick() }
    }

    @UnstableDefault
    private fun findButtonOnClick() {
        //Log.i("findUser", "onClick")
        if (nicknameEdit.text.isNotEmpty()) {
            //Log.i("findUser", "not empty")
            var nickname = nicknameEdit.text.toString()
            if (isNicknameValid(nickname)) {
                //Log.i("findUser", "nickname is valid")
                //TODO поиск пользователей
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
                                var res1 = functions!!
                                    .getHttpsCallable("getUser")
                                    .call(data).addOnCompleteListener { task1 ->
                                        try {
                                            Log.i("findUser:get", "result " + task1.result?.data.toString())
                                            result = task1.result?.data as HashMap<String, Any>
                                            id = result["id"] as String
                                            try {
                                                nickname = result["nickname"] as String
                                            } catch (e: Exception) {
                                                nickname = ""
                                                Log.i("findUser:get", e.message)
                                            }
                                            val avatar = try {
                                                result["avatar"] as String
                                            } catch (e: Exception) {
                                                null
                                            }
                                            if (avatar != null) {
                                                val bitmap = LruCache(c!!)[avatar]
                                                if (bitmap != null) {
                                                    avatarView.setImageBitmap(bitmap)
                                                } else {
                                                    Picasso.get().load(avatar).into(object : com.squareup.picasso.Target {
                                                        override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                                            avatarView.setImageBitmap(bitmap)
                                                        }

                                                        override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                                                        override fun onBitmapFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
                                                            Log.i("FriendListAdapter", "Загрузка изображения не удалась " + avatar + "\n" + e?.message)
                                                        }
                                                    })
                                                }
                                            } else {
                                                Log.i("findUser", "avatar wasn't received")
                                            }
                                            var rn = ""
                                            try {
                                                rn = result["realname"] as String
                                            } catch (e: Exception) {
                                                rn = ""
                                                Log.i("findUser:get", e.message)
                                            }
//                                            nickname = try {
//                                                result["nickname"] as String
//                                            } catch (e: Exception) {
//                                                ""
//                                            }
//                                            val avatar = try {
//                                                result["avatar"] as String
//                                            } catch (e: Exception) {
//                                                ""
//                                            }
                                            makeVisibleFound()
                                            foundNickname.text = nickname
                                            realNameEdit.text = rn
                                            //makeVisibleFound()
                                            Log.i("findUser:get", "id $id")
                                            Log.i("findUser:get", "nickname $nickname")
                                            Log.i("findUser:get", "real name $rn")
                                            Log.i("findUser:get", "avatar $avatar")
                                        } catch (e: Exception) {
                                            Log.i("findUser:get", "error " + e.message)
                                        }
                                        //messageInput.setText("")
                                    }
                            } else {
                                Log.i("findUser:find", "Пользователь с ником $nickname не найден")
                                Toast.makeText(c!!, R.string.toast_user_nickname_not_found, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.i("findUser:find", "error " + e.message)
                        }
                        //messageInput.setText("")
                    }
            } else {
                Toast.makeText(c!!, R.string.toast_nickname_not_valid, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun inviteButtonOnClick() {
        when {
            foundId.isNotEmpty() -> {
                //val data = mapOf("invites" to arrayOf(mAuth!!.uid))
                val uid = mAuth!!.uid
                val data = mapOf("inviterId" to mAuth!!.uid, "id" to foundId)
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
        avatarView.visibility = View.GONE
        foundNickname.visibility = View.GONE
        realNameLabel.visibility = View.GONE
        realNameEdit.visibility = View.GONE
        inviteButton.visibility = View.GONE
    }

    fun makeVisibleFound() {
        found.visibility = View.VISIBLE
        avatarView.visibility = View.VISIBLE
        foundNickname.visibility = View.VISIBLE
        realNameLabel.visibility = View.VISIBLE
        realNameEdit.visibility = View.VISIBLE
        inviteButton.visibility = View.VISIBLE
    }
}

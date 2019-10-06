package com.jimipurple.himichat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.android.synthetic.main.activity_find_friend.*
import kotlinx.android.synthetic.main.activity_find_friend.nicknameEdit
import kotlinx.android.synthetic.main.activity_find_friend.realNameEdit
import kotlinx.android.synthetic.main.activity_find_friend.realNameLabel
import java.util.regex.Pattern
import kotlinx.serialization.*


class FindFriendActivity : BaseActivity() {

    private var mAuth: FirebaseAuth? = null
    private var firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private var firebaseToken: String  = ""
    private var functions = FirebaseFunctions.getInstance()

    var foundId = "" // id найденного пользователя

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_friend)

        mAuth = FirebaseAuth.getInstance()

        friendsButton.setOnClickListener { friendsButtonOnClick() }
        dialoguesButton.setOnClickListener { dialoguesButtonOnClick() }
        settingsButton.setOnClickListener { settingsButtonOnClick() }
        findButton.setOnClickListener { findButtonOnClick() }
        inviteButton.setOnClickListener { inviteButtonOnClick() }


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
                var res = functions
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
                                var res1 = functions
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
                                                ""
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
                                Toast.makeText(this, R.string.toast_user_nickname_not_found, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Log.i("findUser:find", "error " + e.message)
                        }
                        //messageInput.setText("")
                    }
            } else {
                Toast.makeText(this, R.string.toast_nickname_not_valid, Toast.LENGTH_SHORT).show()
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
                functions
                    .getHttpsCallable("inviteUser")
                    .call(data)
                    .continueWith { task ->
                        // This continuation runs on either success or failure, but if the task
                        // has failed then result will throw an Exception which will be
                        // propagated down.
                        val result = task.result?.data as HashMap<String, Any>
                        if (result["invite"] == true) {
                            Toast.makeText(this, R.string.toast_invite_successful, Toast.LENGTH_LONG).show()
                        } else if (result["invite"] == false) {
                            if (result["reason"] == "already invited") {
                                Toast.makeText(this, R.string.toast_invite_already, Toast.LENGTH_LONG).show()
                            } else if (result["reason"] == "already invited you") {
                                Toast.makeText(this, R.string.toast_invite_already_you, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
            }
            mAuth!!.uid == foundId -> {
                Toast.makeText(this, R.string.toast_selfinvite, Toast.LENGTH_LONG).show()
                Log.i("inviteUser::failure", "foundId == uid")
            }
            else -> {
                Toast.makeText(this, R.string.toast_no_found_user, Toast.LENGTH_LONG).show()
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

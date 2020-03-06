package com.jimipurple.himichat.utills

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.jimipurple.himichat.models.User
import kotlinx.coroutines.runBlocking

class FirestoreRequest(val context: Context) {
    fun getUser(id: String) : User? {
        var user: User? = null
        runBlocking {
            FirebaseFirestore.getInstance().collection("users").document(id).get().addOnCompleteListener{
                if (it.isSuccessful) {
                    val userData = it.result!!
                    var nickname = userData.get("nickname") as String?
                    if (nickname == null) {
                        nickname = ""
                    }
                    var realname = userData.get("real_name") as String?
                    if (realname == null) {
                        realname = ""
                    }
                    var avatar = userData.get("avatar") as String?
                    if (avatar == null) {
                        avatar = ""
                    }
                    user = User(id, nickname, realname, avatar)
                } else {
                    Log.i("FirestoreRequest", "Error getting documents.", it.exception)
                }
            }
        }
        Log.i("FirestoreRequest", "getUser " + user.toString())
        return user
    }
}
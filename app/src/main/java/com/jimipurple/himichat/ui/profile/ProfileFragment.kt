package com.jimipurple.himichat.ui.profile

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.User
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_profile.*


class ProfileFragment : BaseFragment() {

    val REQUEST_CODE_DIALOG_ACTIVITY = 1
    var friend_id : String? = null
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
        friend_id = arguments!!["friend_id"] as String
        firestore!!.collection("users").document(friend_id!!).get().addOnCompleteListener{
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
                val user = User(friend_id!!, nickname!!, realname!!, avatar!!)

                Picasso.get().load(avatar).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        if (profileAvatarView != null) {
                            profileAvatarView.setImageBitmap(bitmap)
                        }
                        LruCache(c!!).set(avatar!!, bitmap!!)
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        Log.i("Profile", "Загрузка изображения не удалась " + avatar + "\n" + e?.message)
                    }
                })
                nicknameEdit.setText(nickname)
                realnameEdit.setText(realname)
            } else {
                Log.i("FirestoreRequest", "Error getting documents.", it.exception)
            }
        }
    }
}

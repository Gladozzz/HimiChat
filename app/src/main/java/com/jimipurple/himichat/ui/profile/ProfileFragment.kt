package com.jimipurple.himichat.ui.profile

import android.R.attr.label
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FieldValue
import com.jimipurple.himichat.BaseFragment
import com.jimipurple.himichat.R
import com.squareup.picasso.LruCache
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
            navController.navigate(R.id.nav_dialog, b, navOptions)
        }
        profileEditButton.setOnClickListener {
            val b = Bundle()
            val navController = findNavController()
            navController.navigate(R.id.nav_settings, b, navOptions)
        }
        profileInviteButton.setOnClickListener {
            fbSource!!.inviteToFriends(profile_id!!, {
//                requireActivity().recreate()
            }, { e ->
                if (e.message == "already invited you") {
                    //
                }
            })
        }
        profileRemoveFriendButton.setOnClickListener {
            fbSource!!.removeFriend(profile_id!!, {
//                requireActivity().recreate()
            })
        }
        fbSource!!.isAdmin(fbSource!!.uid()!!, { isAdmin ->
            fbSource!!.getUser(fbSource!!.uid()!!, { currentUser ->
                fbSource!!.getUser(profile_id!!, { user ->
                    if (isAdmin) {
                        setNotFavoriteProfileMode()
                        if (currentUser.favorites != null) {
                            for (i in currentUser.favorites!!) {
                                if (profile_id == i) {
                                    setFavoriteProfileMode()
                                }
                            }
                        }
                    } else {
                        if (currentUser.friends != null) {
                            for (i in currentUser.friends!!) {
                                if (profile_id == i) {
                                    setFriendProfileMode()
                                }
                            }
                        }
                    }
                    nickname = user.nickname
                    realname = user.realName
                    avatar = user.avatar

                    val clipboard: ClipboardManager? =  c!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    nicknameInput.setOnLongClickListener {
                        val clip = ClipData.newPlainText("nickname of opened user profile", nickname)
                        clipboard!!.setPrimaryClip(clip)
                        Toast.makeText(c!!, R.string.toast_text_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        return@setOnLongClickListener true
                    }
                    realnameInput.setOnLongClickListener {
                        val clip = ClipData.newPlainText("real name of opened user profile", realname)
                        clipboard!!.setPrimaryClip(clip)
                        Toast.makeText(c!!, R.string.toast_text_copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        return@setOnLongClickListener true
                    }

                    Glide.with(this)
                        .asBitmap()
                        .load(avatar)
                        .into(object : CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                bitmap: Bitmap,
                                transition: Transition<in Bitmap>?
                            ) {
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
                })
            })
        })
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

    private fun setFavoriteProfileMode() {
        if (profileSendMessageButton != null) {
            profileSendMessageButton.visibility = View.VISIBLE
        }
        if (profileRemoveFavoriteButton != null) {
            profileRemoveFavoriteButton.visibility = View.VISIBLE
        }

        if (profileEditButton != null) {
            profileEditButton.visibility = View.GONE
        }
        if (profileAddToFavoriteButton != null) {
            profileAddToFavoriteButton.visibility = View.GONE
        }
        if (profileInviteButton != null) {
            profileInviteButton.visibility = View.GONE
        }
        if (profileRemoveFriendButton != null) {
            profileRemoveFriendButton.visibility = View.GONE
        }
    }

    private fun setNotFavoriteProfileMode() {
        if (profileSendMessageButton != null) {
            profileSendMessageButton.visibility = View.VISIBLE
        }
        if (profileAddToFavoriteButton != null) {
            profileAddToFavoriteButton.visibility = View.VISIBLE
        }

        if (profileEditButton != null) {
            profileEditButton.visibility = View.GONE
        }
        if (profileRemoveFavoriteButton != null) {
            profileRemoveFavoriteButton.visibility = View.GONE
        }
        if (profileInviteButton != null) {
            profileInviteButton.visibility = View.GONE
        }
        if (profileRemoveFriendButton != null) {
            profileRemoveFriendButton.visibility = View.GONE
        }
    }
}

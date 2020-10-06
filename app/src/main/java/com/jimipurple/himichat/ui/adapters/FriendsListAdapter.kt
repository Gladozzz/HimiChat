package com.jimipurple.himichat.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import cn.carbs.android.avatarimageview.library.AvatarImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.*
import com.squareup.picasso.LruCache


class FriendsListAdapter(val context: Context, var items: List<User>, val profile: (User) -> Unit, val dialog: (u: User)-> Unit) : RecyclerView.Adapter<FriendsListAdapter.FriendHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendHolder {
        Log.i("FriendListAdapter", "items $items")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friends_list, parent, false)
        return FriendHolder(view)
    }

    fun clearItems() {
        items = listOf()
    }

    fun sortFriends(notify: Boolean = true) {
        items = items.sortedWith(friendsComporator)
        if (notify) {
            this.notifyDataSetChanged()
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: FriendHolder, position: Int) {
        holder.bind(items[position])
        Log.i("FriendListAdapter", "onBindViewHolder")
    }

    inner class FriendHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val name = itemView.findViewById(R.id.name) as TextView
        private val realName = itemView.findViewById(R.id.realName) as TextView
        private val avatar = itemView.findViewById(R.id.avatarFriend) as AvatarImageView

        fun bind(item: User) {
            name.text = item.nickname
            realName.text = item.realName

            if (item.avatar.isNotEmpty()) {
                val url = Uri.parse(item.avatar)
                if (url != null) {
                    val bitmap = LruCache(context.applicationContext)[item.avatar]
                    if (bitmap != null) {
                        avatar.setImageBitmap(bitmap)
                    } else {
                        Glide.with(context)
                            .asBitmap()
                            .load(url)
                            .into(object : CustomTarget<Bitmap>(){
                                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                    avatar.setImageBitmap(resource)
                                    LruCache(context.applicationContext).set(url.toString(), resource)
                                }
                                override fun onLoadCleared(placeholder: Drawable?) {
                                    //
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    super.onLoadFailed(errorDrawable)
                                    Log.e("FriendListAdapter", "Загрузка изображения не удалась $url")
                                }
                            })
                    }
                } else {
                    Log.i("FriendListAdapter", "avatar wasn't received")
                }
            }

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) dialog(item)
            }

            avatar.setOnClickListener { profile(item)}
        }
    }

    private val friendsComporator = Comparator<User> { a, b ->
        when {
            (false) -> 0
            a.nickname < b.nickname -> 1
            a.nickname > b.nickname -> -1
            else -> 1
        }
    }

    interface Callback {
        fun onItemClicked(item: User)
    }
}
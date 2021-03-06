package com.jimipurple.himichat.ui.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.jimipurple.himichat.R
import com.jimipurple.himichat.data.FirebaseSource
import com.jimipurple.himichat.models.*
import com.liangfeizc.avatarview.AvatarView
import com.squareup.picasso.LruCache
import net.sectorsieteg.avatars.AvatarDrawableFactory

class UsersListAdapter(
    val context: Context,
    var items: List<User>,
    val profile: (User) -> Unit,
    val dialog: (u: User) -> Unit
) : RecyclerView.Adapter<UsersListAdapter.UsersHolder>() {

    val tag = "UsersAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersHolder {
        Log.d(tag, "items $items")
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.users_list, parent, false)
        return UsersHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: UsersHolder, position: Int) {
        holder.bind(items[position])
        Log.d(tag, "onBindViewHolder")
    }

    fun clearItems() {
        items = listOf()
    }

    fun sortUsers(notify: Boolean = true) {
        items = items.sortedWith(userComporator)
        if (notify) {
            this.notifyDataSetChanged()
        }
    }

    inner class UsersHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val name = itemView.findViewById(R.id.nameUsers) as TextView
        private val realName = itemView.findViewById(R.id.realNameUsers) as TextView
        private val avatar = itemView.findViewById(R.id.avatarUsers) as AvatarView
        private val favoriteButton = itemView.findViewById(R.id.favoriteButton) as ImageButton

        fun bind(item: User) {
            val fbSource = FirebaseSource(context)
            name.text = item.nickname
            realName.text = item.realName
            fun onClick() {
                if (item.favorite == true) {
                    fbSource.removeFavorite(item.id, {
                        favoriteButton.setImageResource(android.R.drawable.btn_star_big_off)
                        item.favorite = false
                        sortUsers()
                    })
                } else {
                    fbSource.addToFavorite(item.id, {
                        favoriteButton.setImageResource(android.R.drawable.btn_star_big_on)
                        item.favorite = true
                        sortUsers()
                    })
                }
            }

            fbSource.isFavorite(item.id, { isFavorite ->
                if (isFavorite) {
                    favoriteButton.setImageResource(android.R.drawable.btn_star_big_on)
                    item.favorite = true
                } else {
                    item.favorite = false
                }
                favoriteButton.setOnClickListener {
                    onClick()
                }
            }, {
                item.favorite = false
                favoriteButton.setOnClickListener {
                    onClick()
                }
            })


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
                            .into(object : CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: Transition<in Bitmap>?
                                ) {
                                    val options = BitmapFactory.Options()
                                    options.inMutable = false
                                    val avatarFactory = AvatarDrawableFactory(context.resources)
                                    val avatarDrawable =
                                        avatarFactory.getRoundedAvatarDrawable(resource)
                                    avatar.setImageDrawable(avatarDrawable)
                                    LruCache(context).set(url.toString(), avatarDrawable.toBitmap())
                                }

                                override fun onLoadCleared(placeholder: Drawable?) {
                                    //
                                }

                                override fun onLoadFailed(errorDrawable: Drawable?) {
                                    super.onLoadFailed(errorDrawable)
                                    Log.e(tag, "Загрузка изображения не удалась $url")
                                }
                            })
                    }
                } else {
                    Log.d(tag, "avatar wasn't received")
                }
            }

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) dialog(item)
            }

            avatar.setOnClickListener { profile(item) }
        }
    }

    interface Callback {
        fun onItemClicked(item: User)
    }
}

private val userComporator = Comparator<User> { a, b ->
    when {
        (false) -> 0
        ((a.favorite == null && b.favorite == null || a.favorite == false && b.favorite == null || a.favorite == null && b.favorite == false) && a.nickname < b.nickname) -> 1
        ((a.favorite == null && b.favorite == null || a.favorite == false && b.favorite == null || a.favorite == null && b.favorite == false) && a.nickname > b.nickname) -> -1
        (a.favorite == true && b.favorite == true && a.nickname < b.nickname) -> 1
        (a.favorite == true && b.favorite == true && a.nickname > b.nickname) -> -1
        ((a.favorite == false && b.favorite == true || a.favorite == null && b.favorite == true) && (a.nickname < b.nickname || a.nickname > b.nickname)) -> 1
        ((a.favorite == true && b.favorite == false || a.favorite == true && b.favorite == null) && (a.nickname < b.nickname || a.nickname > b.nickname)) -> -1
        else -> 1
    }
}

//private fun <User> List<User>.sortedWith(comparator: (User) -> Int): List<User> {
//    TODO("Not yet implemented")
//}

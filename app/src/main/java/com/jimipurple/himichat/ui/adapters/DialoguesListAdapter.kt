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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.Dialog
import com.liangfeizc.avatarview.AvatarView
import com.squareup.picasso.LruCache
import com.squareup.picasso.Picasso
import net.sectorsieteg.avatars.AvatarDrawableFactory


class DialoguesListAdapter(
    val context: Context,
    var items: ArrayList<Dialog>,
    val clickCallback: Callback?,
    val onHoldCallback: (d: Dialog) -> Unit
) : RecyclerView.Adapter<DialoguesListAdapter.DialogHolder>() {

//    private fun hashMapToUser(h : ArrayList<HashMap<String, Any>>) : ArrayList<User> {
//        val u : ArrayList<User> = ArrayList<User>()
//        h.forEach {
//            u.add(User(it["id"] as String, it["nickname"] as String, it["realname"] as String, it["avatar"] as String))
//        }
//        Log.i("convert", h.toString())
//        Log.i("convert", u.toString())
//        return u
//    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DialogHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.dialog_list, parent, false)
        return DialogHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: DialogHolder, position: Int) {
        holder.bind(items[position])
        Log.i("Recycler", "items $items")
    }

    inner class DialogHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val name = itemView.findViewById(R.id.name) as TextView
        private val avatar = itemView.findViewById(R.id.avatarDialog) as AvatarView
        private val lastMessage = itemView.findViewById(R.id.lastMessage) as TextView

        fun bind(item: Dialog) {
            name.text = item.nickname
            if (item.lastMessage != null) {
                lastMessage.text = item.lastMessage.text
            }

            if (item.avatar != null && item.nickname != null) {
                val url = Uri.parse(item.avatar)
                if (url != null) {
                    val bitmap = LruCache(context)[item.avatar!!]
                    if (bitmap != null) {
                        avatar.setImageBitmap(bitmap)
                    } else {
                        Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                            override fun onBitmapLoaded(
                                bitmap: Bitmap?,
                                from: Picasso.LoadedFrom?
                            ) {
//                                val options = BitmapFactory.Options()
//                                options.inMutable = false
//                                val avatarFactory = AvatarDrawableFactory(context.resources)
//                                val avatarDrawable =
//                                    avatarFactory.getRoundedAvatarDrawable(bitmap)
                                if (bitmap != null) {
                                    avatar.setImageBitmap(bitmap)
                                    LruCache(context).set(url.toString(), bitmap)
                                }
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.i(
                                    "DialoguesListAdapter",
                                    "Загрузка изображения не удалась " + avatar + "\n" + e?.message
                                )
                            }
                        })
                    }
                } else {
                    Log.i("DialoguesListAdapter", "avatar wasn't received")
                }
            }

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback?.onItemClicked(items[adapterPosition])
            }
        }
    }

    interface Callback {
        fun onItemClicked(item: Dialog)
    }
}
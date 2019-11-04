package com.jimipurple.himichat.adapters

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
import com.google.android.gms.tasks.Task
import com.jimipurple.himichat.R
import com.squareup.picasso.Picasso
import com.jimipurple.himichat.models.*
import com.squareup.picasso.LruCache
import java.lang.Exception


class FriendRequestsListAdapter(val context: Context, var items: ArrayList<FriendRequest>, val clickCallback: Callback, val cancelCallback: (fr: FriendRequest)-> Task<Unit>, val blockCallback: (fr: FriendRequest)-> Task<Unit>, val acceptCallback: (fr: FriendRequest)->Task<Unit>) : RecyclerView.Adapter<FriendRequestsListAdapter.BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == ItemViewType.RECEIVED_FRIEND_REQUEST) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.received_requests_list, parent, false)
            ReceivedFriendRequestHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.sent_requests_list, parent, false)
            SentFriendRequestHolder(view)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(items[position])
        Log.i("Recycler", "items $items")
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isReceived) {
            ItemViewType.RECEIVED_FRIEND_REQUEST
        } else {
            ItemViewType.SENT_FRIEND_REQUEST
        }
    }

    inner class ReceivedFriendRequestHolder(itemView: View) : BaseViewHolder(itemView) {

        private val name = itemView.findViewById(R.id.name) as TextView
        private val realName = itemView.findViewById(R.id.realName) as TextView
        private val avatar = itemView.findViewById(R.id.avatarReceivedRequest) as ImageView
        private val blockButton = itemView.findViewById(R.id.blockButton) as ImageButton
        private val acceptButton = itemView.findViewById(R.id.acceptButton) as ImageButton

        override fun bind(item: FriendRequest) {
            name.text = item.nickname
            realName.text = item.realName
            Log.i("Recycler", "all must be ok")
            Log.i("Recycler", "item $item")

            if (item.avatar.isNotEmpty()) {
                val url = Uri.parse(item.avatar)
                if (url != null) {
                    val bitmap = LruCache(context)[item.avatar]
                    if (bitmap != null) {
                        avatar.setImageBitmap(bitmap)
                    } else {
                        Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                avatar.setImageBitmap(bitmap)
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.i("FriendListAdapter", "Загрузка изображения не удалась " + avatar + "\n" + e?.message)
                            }
                        })
                    }
                } else {
                    Log.i("FriendListAdapter", "avatar wasn't received")
                }
            }

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
            }

            blockButton.setOnClickListener {blockCallback(item)}

            acceptButton.setOnClickListener {acceptCallback(item)}
        }
    }

    inner class SentFriendRequestHolder(itemView: View) : BaseViewHolder(itemView) {

        private val name = itemView.findViewById(R.id.name) as TextView
        private val realName = itemView.findViewById(R.id.realName) as TextView
        private val avatar = itemView.findViewById(R.id.avatarSentRequest) as ImageView
        private val cancelButton = itemView.findViewById(R.id.cancelButton) as ImageButton

        override fun bind(item: FriendRequest) {
            name.text = item.nickname
            realName.text = item.realName

            if (item.avatar.isNotEmpty()) {
                val url = Uri.parse(item.avatar)
                if (url != null) {
                    val bitmap = LruCache(context)[item.avatar]
                    if (bitmap != null) {
                        avatar.setImageBitmap(bitmap)
                    } else {
                        Picasso.get().load(url).into(object : com.squareup.picasso.Target {
                            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                                avatar.setImageBitmap(bitmap)
                                LruCache(context).set(url.toString(), bitmap!!)
                            }

                            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                                Log.i("FriendListAdapter", "Загрузка изображения не удалась " + avatar + "\n" + e?.message)
                            }
                        })
                    }
                } else {
                    Log.i("FriendListAdapter", "avatar wasn't received")
                }
            }

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
            }

            cancelButton.setOnClickListener {cancelCallback(item)}
        }
    }

    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: FriendRequest)
    }

    interface Callback {
        fun onItemClicked(item: FriendRequest)
    }

    interface ItemViewType {
        companion object {
            val RECEIVED_FRIEND_REQUEST = 0
            val SENT_FRIEND_REQUEST = 1
        }
    }
}
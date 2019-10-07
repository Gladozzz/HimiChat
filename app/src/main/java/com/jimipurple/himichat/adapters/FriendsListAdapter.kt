package com.jimipurple.himichat.adapters

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
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





class FriendsListAdapter(var items: ArrayList<User>, val clickCallback: Callback, val sendMessageButtonOnClick: (u: User)-> Task<Unit>) : RecyclerView.Adapter<FriendsListAdapter.FriendHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.friends_list, parent, false)
        return FriendHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: FriendHolder, position: Int) {
        holder.bind(items[position])
        Log.i("Recycler", "items $items")
    }

    inner class FriendHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val name = itemView.findViewById(R.id.name) as TextView
        private val realName = itemView.findViewById(R.id.realName) as TextView
        private val avatar = itemView.findViewById(R.id.avatarFriend) as ImageView
        private val sendMessageButton = itemView.findViewById(R.id.sendMessageButton) as ImageButton

        fun bind(item: User) {
            name.text = item.nickname
            realName.text = item.realName
            Log.i("Recycler", "all must be ok")
            Log.i("Recycler", "item $item")

            if (item.avatar.isNotEmpty()) {
                Picasso.get().load(item.avatar).into(object : com.squareup.picasso.Target {
                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                        avatar.setImageBitmap(bitmap)
                    }

                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                        Log.i("FriendListAdapter", "Загрузка изображения не удалась " + item.avatar + "\n" + e?.message)
                    }
                })
            }

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
            }

            sendMessageButton.setOnClickListener {sendMessageButtonOnClick(item)}
        }
    }

    interface Callback {
        fun onItemClicked(item: User)
    }
}
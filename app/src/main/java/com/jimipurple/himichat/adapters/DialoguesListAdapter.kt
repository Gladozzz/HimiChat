package com.jimipurple.himichat.adapters

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jimipurple.himichat.R
import com.jimipurple.himichat.models.Dialog
import com.squareup.picasso.Picasso

class DialoguesListAdapter(var items: ArrayList<Dialog>, val clickCallback: Callback, val onHoldCallback: (d: Dialog)-> Unit) : RecyclerView.Adapter<DialoguesListAdapter.DialogHolder>() {

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
        private val avatar = itemView.findViewById(R.id.avatarDialog) as ImageView
        private val lastMessage = itemView.findViewById(R.id.lastMessage) as TextView

        fun bind(item: Dialog) {
            name.text = item.nickname
            lastMessage.text = item.lastMessage
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
        }
    }

    interface Callback {
        fun onItemClicked(item: Dialog)
    }
}
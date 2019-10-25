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





class MessageListAdapter(var items: ArrayList<Message>, val clickCallback: Callback, val cancelCallback: (fr: Message)-> Task<Unit>, val blockCallback: (fr: Message)-> Task<Unit>, val acceptCallback: (fr: Message)->Task<Unit>) : RecyclerView.Adapter<MessageListAdapter.BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == ItemViewType.RECEIVED_MESSAGE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.received_requests_list, parent, false)
            ReceivedMessageHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.sent_requests_list, parent, false)
            SentMessageolder(view)
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(items[position])
        Log.i("Recycler", "items $items")
    }

    override fun getItemViewType(position: Int): Int {
        if (items[position] is ReceivedMessage) {
            return ItemViewType.RECEIVED_MESSAGE
        } else if (items[position] is SentMessage){
            return ItemViewType.SENT_MESSAGE
        }
        return ItemViewType.UNDELIVERED_MESSAGE
    }

    inner class ReceivedMessageHolder(itemView: View) : BaseViewHolder(itemView) {

        private val text = itemView.findViewById(R.id.name) as TextView
        private val date = itemView.findViewById(R.id.sentMessageDate) as TextView
        private val avatar = itemView.findViewById(R.id.avatarReceivedRequest) as ImageView
        private val blockButton = itemView.findViewById(R.id.blockButton) as ImageButton
        private val acceptButton = itemView.findViewById(R.id.acceptButton) as ImageButton

        override fun bind(item: Message) {
            val i = item as ReceivedMessage
            text.text = item.text
            val dateStr = i.date!!.hours.toString() + "." + i.date!!.minutes.toString() + "." + i.date!!.day.toString() + "." + i.date!!.month.toString() + "." + (i.date!!.year + 1900).toString()
            date.text = dateStr
            Log.i("Recycler", "all must be ok")
            Log.i("Recycler", "item $i")

//            if (item.avatar.isNotEmpty()) {
//                Picasso.get().load(item.avatar).into(object : com.squareup.picasso.Target {
//                    override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
//                        avatar.setImageBitmap(bitmap)
//                    }
//
//                    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}
//
//                    override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
//                        Log.i("FriendListAdapter", "Загрузка изображения не удалась " + item.avatar + "\n" + e?.message)
//                    }
//                })
//            }

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
            }

            blockButton.setOnClickListener {blockCallback(i)}

            acceptButton.setOnClickListener {acceptCallback(i)}
        }
    }

    inner class SentMessageolder(itemView: View) : BaseViewHolder(itemView) {

        private val text = itemView.findViewById(R.id.sentMessageDate) as TextView
        private val date = itemView.findViewById(R.id.sentMessageText) as TextView

        override fun bind(item: Message) {
            text.text = item.text
            date.text = item.text

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
            }
        }
    }

    inner class UnreceivedMessageolder(itemView: View) : BaseViewHolder(itemView) {

        private val text = itemView.findViewById(R.id.unreceivedMessageText) as TextView

        override fun bind(item: Message) {
            text.text = item.text

            itemView.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
            }

            //cancelButton.setOnClickListener {cancelCallback(item)}
        }
    }

    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        abstract fun bind(item: Message)
    }

    interface Callback {
        fun onItemClicked(item: Message)
    }

    interface ItemViewType {
        companion object {
            val RECEIVED_MESSAGE = 0
            val SENT_MESSAGE = 1
            val UNDELIVERED_MESSAGE = 2
        }
    }
}
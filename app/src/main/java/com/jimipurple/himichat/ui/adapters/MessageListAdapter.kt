package com.jimipurple.himichat.ui.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.jimipurple.himichat.R
import com.jimipurple.himichat.db.MessagesDBHelper
import com.jimipurple.himichat.models.*
import java.text.SimpleDateFormat


class MessageListAdapter(val context: Context, var items: ArrayList<Message>, val clickCallback: Callback, val deleteCallback: (msg: Message)-> Unit, val editCallback: (msg: Message)-> Unit, val onHoldCallback: (msg: Message)->Unit) : RecyclerView.Adapter<MessageListAdapter.BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (viewType) {
            ItemViewType.RECEIVED_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.received_message_list, parent, false)
                ReceivedMessageHolder(view)
            }
            ItemViewType.SENT_MESSAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.sent_message_list, parent, false)
                SentMessageHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.unreceived_message_list, parent, false)
                UnreceivedMessageHolder(view)
            }
        }
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        holder.bind(items[position])
        Log.i("Recycler", "items $items")

        when (holder.itemViewType) {
            ItemViewType.RECEIVED_MESSAGE -> {
                holder.itemView.setOnCreateContextMenuListener { contextMenu, _, _ ->
                    contextMenu.add(context.getString(R.string.context_menu_item_remove)).setOnMenuItemClickListener {
                        deleteCallback(items[position])
                        true
                    }
                }
            }

            ItemViewType.SENT_MESSAGE -> {
                holder.itemView.setOnCreateContextMenuListener { contextMenu, _, _ ->
                    contextMenu.add(context.getString(R.string.context_menu_item_remove)).setOnMenuItemClickListener {
                        deleteCallback(items[position])
                        true
                    }
                }
            }

            else -> {
                holder.itemView.setOnCreateContextMenuListener { contextMenu, _, _ ->
                    contextMenu.add(context.getString(R.string.context_menu_item_remove)).setOnMenuItemClickListener {
                        deleteCallback(items[position])
                        true
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        when (items[position]) {
            is ReceivedMessage -> {
                return ItemViewType.RECEIVED_MESSAGE
            }
            is SentMessage -> {
                return ItemViewType.SENT_MESSAGE
            }
            else -> {
                return ItemViewType.UNDELIVERED_MESSAGE
            }
        }    }

    inner class ReceivedMessageHolder(itemView: View) : BaseViewHolder(itemView) {

        private val text = itemView.findViewById(R.id.receivedMessageText) as TextView
        private val date = itemView.findViewById(R.id.receivedMessageDate) as TextView

        override fun bind(item: Message) {
            val i = item as ReceivedMessage
            text.text = item.text
            val d = i.date
            val dateTime = i.date
            Log.i("messagesAdapter", "dateTime $dateTime")
            Log.i("messagesAdapter", "dateLong $d")
            val df = SimpleDateFormat("HH:mm")
            val formattedDate = df.format(d)
            date.text = formattedDate

//            itemView.setOnClickListener {
//                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
//            }

            //blockButton.setOnClickListener {blockCallback(i)}

            //acceptButton.setOnClickListener {acceptCallback(i)}
        }
    }

    inner class SentMessageHolder(itemView: View) : BaseViewHolder(itemView) {

        private val text = itemView.findViewById(R.id.sentMessageText) as TextView
        private val date = itemView.findViewById(R.id.sentMessageDate) as TextView

        override fun bind(item: Message) {
            val i = item as SentMessage
            text.text = i.text
            val d = i.date
            val dateTime = i.date
            Log.i("messagesAdapter", "dateTime $dateTime")
            Log.i("messagesAdapter", "dateLong $d")
            //val df = SimpleDateFormat("dd-MMM-yyyy")
            val df = SimpleDateFormat("HH:mm")
            val formattedDate = df.format(d)
            date.text = formattedDate

//            itemView.setOnClickListener {
//                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
//            }
        }
    }

    inner class UnreceivedMessageHolder(itemView: View) : BaseViewHolder(itemView) {

        private val text = itemView.findViewById(R.id.unreceivedMessageText) as TextView
        private val send = itemView.findViewById(R.id.sendUnreceivedMessageButton) as ProgressBar

        override fun bind(item: Message) {
            text.text = item.text

//            itemView.setOnClickListener {
//                if (adapterPosition != RecyclerView.NO_POSITION) clickCallback.onItemClicked(items[adapterPosition])
//            }
            send.setOnClickListener {
                send.isClickable = false
                val mAuth = FirebaseAuth.getInstance()
                val functions = FirebaseFunctions.getInstance()
                val id = mAuth.uid!!
                val db = MessagesDBHelper(context)
                val text = item.text
                val receiverId = item.receiverId
                val senderId = item.senderId
                val msg = UndeliveredMessage(senderId, receiverId, text, (item as UndeliveredMessage).deliveredId)
                //db.pushMessage(msg)
                val data = hashMapOf(
                    "receiverId" to receiverId,
                    "senderId" to senderId,
                    "deliveredId" to msg.deliveredId.toString(),
                    "text" to text,
                    "token" to context.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", "")
                )
                Log.i("msgTest", context.getSharedPreferences("com.jimipurple.himichat.prefs", 0).getString("firebaseToken", ""))
                var res = functions
                    .getHttpsCallable("sendMessage")
                    .call(data).addOnCompleteListener { task ->
                        try {
                            Log.i("dialogMessage", "result " + task.result?.data.toString())
                            data["text"]
                            items.remove(item)
                            send.isClickable = true
                        } catch (e: Exception) {
                            Log.i("dialogMessage", "error " + e.message)
                            send.isClickable = true
                        }
                    }

                Log.i("dialogMessage", "data $data")
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
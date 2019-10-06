package com.jimipurple.himichat.adapters

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.jimipurple.himichat.R
import com.squareup.picasso.Picasso
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import android.content.Context.LAYOUT_INFLATER_SERVICE
import android.view.LayoutInflater
import android.widget.BaseAdapter




class FriendsListAdapter(private val context: Activity, private val userId: ArrayList<String>,  private val name: ArrayList<String>, private val avatar: ArrayList<String>, private val is_registered: ArrayList<String>)
    : ArrayAdapter<String>(context, R.layout.friends_list, name) {

    var pos : Int = 0

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.friends_list, null, true)

        pos = position

        val id: String = userId[position]
        val nameText = rowView.findViewById(R.id.name) as TextView
        val messageText = rowView.findViewById(R.id.lastMessage) as TextView
        val avatarText = rowView.findViewById(R.id.avatarDialog) as ImageView

        nameText.text = name[position]
        messageText.text = is_registered[position]

        var bitmap : Bitmap? = null

        //DownloadImageTask(context, avatar[position]).execute()

        Picasso.get().load(avatar[position]).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                avatarText.setImageBitmap(bitmap)
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                Log.i("FriendListAdapter", "Загрузка изображения не удалась " + avatar[position] + "\n" + e?.message)
            }
        })

        return rowView
    }

    fun getFriend(position: Int): Map<String, String> {
        val name = name[position]
        val uid = userId[position]
        val avatarURL = avatar[position]
        return mapOf("nickname" to name, "userId" to uid, "avatar" to avatarURL)
    }
}

//private class MyCustomAdapter : BaseAdapter() {
//
//    private val mData = ArrayList()
//    private val mInflater: LayoutInflater
//
//    init {
//        mInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
//    }
//
//    fun addItem(item: String) {
//        mData.add(item)
//        notifyDataSetChanged()
//    }
//
//    override fun getCount(): Int {
//        return mData.size()
//    }
//
//    override fun getItem(position: Int): String {
//        return mData.get(position)
//    }
//
//    override fun getItemId(position: Int): Long {
//        return position.toLong()
//    }
//
//    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
//        var convertView = convertView
//        println("getView $position $convertView")
//        var holder: ViewHolder? = null
//        if (convertView == null) {
//            convertView = mInflater.inflate(R.layout.item1, null)
//            holder = ViewHolder()
//            holder.textView = convertView!!.findViewById<View>(R.id.text) as TextView
//            convertView.tag = holder
//        } else {
//            holder = convertView.tag as ViewHolder
//        }
//        holder.textView.setText(mData.get(position))
//        return convertView
//    }
//
//}
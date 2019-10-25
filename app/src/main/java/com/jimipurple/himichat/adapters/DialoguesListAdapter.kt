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

class DialoguesListAdapter(private val context: Activity, private val userId: ArrayList<Int>, private val name: ArrayList<String>, private val avatar: ArrayList<String>, private val is_registered: ArrayList<String>)
    : ArrayAdapter<String>(context, R.layout.friends_list, name) {

    var pos : Int = 0

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.friends_list, null, true)

        pos = position

        val id: Int = userId[position]
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


//    class DownloadImageTask(private val context: Activity, private val urlstring: String) : AsyncTask<String, Void, Bitmap>() {
//        override fun doInBackground(vararg param: String): Bitmap? {
//            try {
//                val url = URL(urlstring)
//                if (urlstring == "") Log.i("FriendListAdapter", "pizdec")
//                val connection = url.openConnection() as HttpURLConnection
//                connection.doInput = true
//                connection.connect()
//                val input = connection.inputStream
//                return BitmapFactory.decodeStream(input)
//            } catch (e: IOException) {
//                Log.i("FriendListAdapter", "Загрузка изображения не удалась " + urlstring + "\n" + e.message)
//            }
//            return null
//        }
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//            // ...
//        }
//
//        override fun onPostExecute(result: Bitmap?) {
//            super.onPostExecute(result)
//            val inflater = context.layoutInflater
//            val rowView = inflater.inflate(R.layout.friends_list, null, true)
//            val avatarText = rowView.findViewById(R.id.avatarDialog) as ImageView
//            if (result == null) Log.i("FriendListAdapter", "pizdec")
//            avatarText.setImageBitmap(result)
//            //Log.i("FriendListAdapter", "complete")
//        }
//    }
}
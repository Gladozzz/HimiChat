package com.jimipurple.himichat.adapters

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.jimipurple.himichat.R
import com.squareup.picasso.Picasso


class SentRequestsListAdapter(private val context: Activity, private val name: ArrayList<String>,  private val realName: ArrayList<String>, private val avatar: ArrayList<String>)
    : ArrayAdapter<String>(context, R.layout.sent_requests_list, name) {

    var pos : Int = 0
    var nickname : String = ""

    override fun getView(position: Int, view: View?, parent: ViewGroup): View {
        val inflater = context.layoutInflater
        val rowView = inflater.inflate(R.layout.friends_list, null, true)

        pos = position

        nickname = name[position]
        val nameText = rowView.findViewById(R.id.name) as TextView
        val realNameText = rowView.findViewById(R.id.realName) as TextView
        val avatarView = rowView.findViewById(R.id.avatarSentRequest) as ImageView

        nameText.text = name[position]
        realNameText.text = realName[position]

        var bitmap : Bitmap? = null

        //DownloadImageTask(context, avatar[position]).execute()

        Picasso.get().load(avatar[position]).into(object : com.squareup.picasso.Target {
            override fun onBitmapLoaded(bitmap: Bitmap?, from: Picasso.LoadedFrom?) {
                avatarView.setImageBitmap(bitmap)
            }

            override fun onPrepareLoad(placeHolderDrawable: Drawable?) {}

            override fun onBitmapFailed(e: Exception?, errorDrawable: Drawable?) {
                Log.i("FriendListAdapter", "Загрузка изображения не удалась " + avatar[position] + "\n" + e?.message)
            }
        })

        return rowView
    }

    fun getNickname(position: Int): String {
        return name[position]
    }
}
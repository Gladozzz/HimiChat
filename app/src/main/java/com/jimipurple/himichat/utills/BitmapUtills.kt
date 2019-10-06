package com.jimipurple.himichat.utills

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import com.squareup.picasso.Picasso



fun getStringFromBitmap(bitmap : Bitmap) : String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    val bytes = stream.toByteArray()
    //val string : String = String(bytes, Charsets.UTF_8)
    val string : String = Base64.encodeToString(bytes, Base64.DEFAULT)
    Log.i("getStringFromBitmap", string)
    return string
}
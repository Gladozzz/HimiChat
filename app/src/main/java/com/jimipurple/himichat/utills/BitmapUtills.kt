package com.jimipurple.himichat.utills

import android.content.Context
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
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
import android.widget.ImageView
import com.jimipurple.himichat.db.BitmapsDBHelper
import java.lang.Exception
import kotlin.concurrent.thread


fun getStringFromBitmap(bitmap : Bitmap) : String {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    val bytes = stream.toByteArray()
    //val string : String = String(bytes, Charsets.UTF_8)
    val string : String = Base64.encodeToString(bytes, Base64.DEFAULT)
    Log.i("getStringFromBitmap", string)
    return string
}


// convert from bitmap to byte array
fun getBytes(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(CompressFormat.PNG, 0, stream)
    return stream.toByteArray()
}

// convert from byte array to bitmap
fun getImage(image: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(image, 0, image.size)
}

fun loadBitmap(context: Context, urlToImage: Uri): Bitmap? {
    try {
        return BitmapsDBHelper(context).getBitmapFromDBorFromUri(urlToImage)
    } catch (e: Exception) {
        Log.i("bitmap", e.message)
        return null
    }
}

fun loadBitmapToImageView(context: Context, urlToImage: Uri, setBitmap: (b: Bitmap) -> Unit) {
    thread() {
        try {
            val bitmap = BitmapsDBHelper(context).getBitmapFromDBorFromUri(urlToImage)
            setBitmap(bitmap!!)
        } catch (e: Exception) {
            Log.i("bitmap load error", e.message)
        }
    }
}
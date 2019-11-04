package com.jimipurple.himichat.utills

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap.CompressFormat
import android.net.Uri
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
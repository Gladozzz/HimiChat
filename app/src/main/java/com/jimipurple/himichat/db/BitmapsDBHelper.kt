package com.jimipurple.himichat.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.net.Uri
import com.squareup.picasso.Picasso
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.provider.BaseColumns
import android.util.Log
import com.jimipurple.himichat.models.Message
import com.jimipurple.himichat.models.ReceivedMessage
import com.jimipurple.himichat.models.SentMessage
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.collections.ArrayList


class BitmapsDBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {

        // creating table
        db.execSQL(CREATE_TABLE_IMAGE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // on upgrade drop older tables
        db.execSQL("DROP TABLE IF EXISTS $DB_TABLE")

        // create new table
        onCreate(db)
    }

    @Throws(SQLiteException::class)
    fun pushBitmapFromUri(urlToImage: Uri) {
        val bitmap = Picasso.get().load(urlToImage).get()
        val stream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 0, stream)
        val byteArray = stream.toByteArray()
        val db = this.writableDatabase
        val cv = ContentValues()
        cv.put(KEY_NAME, urlToImage.toString())
        cv.put(KEY_IMAGE, byteArray)
        db.insert(DB_TABLE, null, cv)
    }

    fun getBitmapFromDBorFromUri(urlToImage: Uri): Bitmap? {
        val db = this.readableDatabase
        val projection = arrayOf(
            KEY_NAME,
            KEY_IMAGE
        )
        val selection = "$KEY_NAME = ?"
        val selectionArgs = arrayOf(urlToImage.toString())
        val sortOrder = "$KEY_NAME DESC"
        val cursor = db.query(
            DB_TABLE,   // The table to query
            projection,             // The array of columns to return (pass null to get all)
            selection,              // The columns for the WHERE clause
            selectionArgs,          // The values for the WHERE clause
            null,                   // don't group the rows
            null,                   // don't filter by row groups
            sortOrder               // The sort order
        )
        if (cursor.moveToFirst()) {
            var bitmap: Bitmap? = null
            val nameColumn = cursor.getColumnIndex(KEY_NAME)
            val imageIdColumn = cursor.getColumnIndex(KEY_IMAGE)
            val name = cursor.getString(nameColumn)
            val byteArray = cursor.getBlob(imageIdColumn)
            cursor.close()
            bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            Log.i("bitmapsDB", "bitmap from db $bitmap")
            return bitmap
        } else {
            pushBitmapFromUri(urlToImage)
            Log.i("bitmapsDB", "bitmap not in db")
            cursor.close()
            return Picasso.get().load(urlToImage).get()
        }
    }

    companion object {
        // Database Version
        private val DATABASE_VERSION = 1

        // Database Name
        private val DATABASE_NAME = "database_name"

        // Table Names
        private val DB_TABLE = "table_image"

        // column names
        private val KEY_NAME = "image_name"
        private val KEY_IMAGE = "image_data"

        // Table create statement
        private val CREATE_TABLE_IMAGE = "CREATE TABLE " + DB_TABLE + "(" +
                KEY_NAME + " TEXT," +
                KEY_IMAGE + " BLOB);"
    }
}
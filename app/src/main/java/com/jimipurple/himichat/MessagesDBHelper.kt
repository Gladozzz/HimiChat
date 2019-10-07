package com.jimipurple.himichat

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns


object DialogMessage : BaseColumns {
    const val TABLE_NAME = "messages"
    const val COLUMN_NAME_ID = "id"
    const val COLUMN_NAME_DIALOG_ID = "dialog_id"
    const val COLUMN_NAME_TEXT = "text"
    const val COLUMN_NAME_SENDER_ID = "sender_id"
}

private const val SQL_CREATE_ENTRIES =
    "CREATE TABLE ${DialogMessage.TABLE_NAME} (" +
            "${BaseColumns._ID} INTEGER PRIMARY KEY," +
            "${DialogMessage.COLUMN_NAME_ID} ID," +
            "${DialogMessage.COLUMN_NAME_DIALOG_ID} ID," +
            "${DialogMessage.COLUMN_NAME_SENDER_ID} TEXT," +
            "${DialogMessage.COLUMN_NAME_TEXT} TEXT)"

private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${DialogMessage.TABLE_NAME}"

class MessagesDBHelperDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
    companion object {
        // If you change the database schema, you must increment the database version.
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "FeedReader.db"
    }
}
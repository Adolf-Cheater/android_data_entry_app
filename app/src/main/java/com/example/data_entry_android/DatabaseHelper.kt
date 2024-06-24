package com.example.data_entry_android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.data_entry_android.model.Entry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "entries.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "entries"
        private const val COLUMN_ID = "id"
        private const val COLUMN_USERNAME = "userName"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_ITEMS = "items"
    }

    private val gson = Gson()

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT,
                $COLUMN_TIMESTAMP TEXT,
                $COLUMN_ITEMS TEXT
            )
        """.trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertEntry(entry: Entry) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, entry.userName)
            put(COLUMN_TIMESTAMP, entry.timestamp)
            put(COLUMN_ITEMS, serializeItems(entry.items))
        }
        db.insert(TABLE_NAME, null, values)
        db.close()
    }

    fun getAllEntries(): List<Entry> {
        val entries = mutableListOf<Entry>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, "$COLUMN_TIMESTAMP DESC")

        cursor.use {
            val userNameIndex = it.getColumnIndex(COLUMN_USERNAME)
            val timestampIndex = it.getColumnIndex(COLUMN_TIMESTAMP)
            val itemsIndex = it.getColumnIndex(COLUMN_ITEMS)

            while (it.moveToNext()) {
                val userName = it.getString(userNameIndex)
                val timestamp = it.getString(timestampIndex)
                val itemsSerialized = it.getString(itemsIndex)
                val items = deserializeItems(itemsSerialized)
                entries.add(Entry(userName, items, timestamp))
                println("Retrieved entry: $userName, $timestamp, ${items.size} items") // Logging
            }
        }
        return entries
    }

    private fun serializeItems(items: List<Pair<String, String>>): String {
        return gson.toJson(items)
    }

    private fun deserializeItems(serializedItems: String): List<Pair<String, String>> {
        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        return gson.fromJson(serializedItems, type)
    }
}
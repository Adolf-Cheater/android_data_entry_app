package com.example.data_entry_android

import android.content.Context
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.example.data_entry_android.model.Entry

class DatabaseHelper(context: Context) {
    // Instance of Retrofit API service
    private val apiService = RetrofitClient.apiService

    // Function to fetch all entries using Retrofit
    fun getAllEntries(callback: (List<Entry>?, String?) -> Unit) {
        apiService.getAllEntries().enqueue(object : Callback<List<Entry>> {
            override fun onResponse(call: Call<List<Entry>>, response: Response<List<Entry>>) {
                if (response.isSuccessful) {
                    // Passing the fetched entries to the callback
                    callback(response.body(), null)
                } else {
                    // Handling response error scenario
                    callback(null, "Failed to retrieve entries: HTTP ${response.code()}")
                }
            }

            override fun onFailure(call: Call<List<Entry>>, t: Throwable) {
                // Handling failure in API call
                callback(null, t.message)
            }
        })
    }

    // Optional: Method to insert an entry via the API if supported
    fun insertEntry(entry: Entry, callback: (Boolean, String?) -> Unit) {
        // Define this method in Retrofit interface if you need to post data to the server
    }

    // Commented out SQLite methods as they are no longer required
    /*
    override fun onCreate(db: SQLiteDatabase) { }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { }
    fun insertEntry(entry: Entry) { }
    private fun serializeItems(items: List<Pair<String, String>>): String { return "" }
    private fun deserializeItems(serializedItems: String): List<Pair<String, String>> { return listOf() }
    */
}







/*
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




    /*
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

     */

    private fun serializeItems(items: List<Pair<String, String>>): String {
        return gson.toJson(items)
    }

    private fun deserializeItems(serializedItems: String): List<Pair<String, String>> {
        val type = object : TypeToken<List<Pair<String, String>>>() {}.type
        return gson.fromJson(serializedItems, type)
    }
}

 */
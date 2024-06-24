package com.example.data_entry_android.model

import java.io.Serializable

data class Entry(
    val userName: String,
    val items: List<Pair<String, String>>,
    val timestamp: String
) : Serializable
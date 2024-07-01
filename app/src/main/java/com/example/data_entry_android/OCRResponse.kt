// OCRResponse.kt
package com.example.data_entry_android

data class OCRResponse(
    val sid: String,
    val prism_version: String,
    val prism_wnum: Int,
    val prism_wordsInfo: List<WordInfo>
)

data class WordInfo(
    val word: String,
    val pos: List<Position>,
    val direction: Int,
    val angle: Int,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class Position(
    val x: Int,
    val y: Int
)
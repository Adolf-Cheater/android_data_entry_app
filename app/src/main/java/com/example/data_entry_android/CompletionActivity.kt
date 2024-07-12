package com.example.data_entry_android

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.data_entry_android.R

class CompletionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completion)

        val imageUri = intent.getStringExtra("IMAGE_URI")
        val ocrResponse = intent.getStringExtra("OCR_RESPONSE")

        Log.d("CompletionActivity", "Received IMAGE_URI: $imageUri")
        Log.d("CompletionActivity", "Received OCR_RESPONSE: $ocrResponse")

        findViewById<Button>(R.id.btnGoToResult).setOnClickListener {
            val intent = Intent(this, OCRResultActivity::class.java).apply {
                putExtra("IMAGE_URI", imageUri)
                putExtra("OCR_RESPONSE", ocrResponse)
            }
            Log.d("CompletionActivity", "Sending to OCRResultActivity - IMAGE_URI: $imageUri")
            Log.d("CompletionActivity", "Sending to OCRResultActivity - OCR_RESPONSE: $ocrResponse")
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            finish()
        }
    }
}
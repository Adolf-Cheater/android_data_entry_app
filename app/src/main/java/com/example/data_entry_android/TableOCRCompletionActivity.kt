package com.example.data_entry_android

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.data_entry_android.R
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class TableOCRCompletionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table_ocr_completion)

        val imageUri = intent.getStringExtra("IMAGE_URI")
        val ocrResponse = intent.getStringExtra("OCR_RESPONSE")

        Log.d("TableOCRCompletionActivity", "Received IMAGE_URI: $imageUri")
        Log.d("TableOCRCompletionActivity", "Received OCR_RESPONSE: $ocrResponse")

        findViewById<Button>(R.id.btnSaveExcel).setOnClickListener {
            ocrResponse?.let { response ->
                saveExcelFile(response)
            } ?: run {
                Toast.makeText(this, "No Excel data to save", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<Button>(R.id.btnBackToMain).setOnClickListener {
            Log.d("TableOCRCompletionActivity", "Back to Main Menu button clicked")
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun saveExcelFile(base64Excel: String) {
        try {
            val decodedExcel = Base64.decode(base64Excel, Base64.DEFAULT)
            val filename = "OCR_Table_Result_${System.currentTimeMillis()}.xlsx"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
                }

                val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(decodedExcel)
                    }
                    Toast.makeText(this, "Excel file saved: $filename", Toast.LENGTH_LONG).show()
                }
            } else {
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                val file = File(documentsDir, filename)
                FileOutputStream(file).use {
                    it.write(decodedExcel)
                }
                Toast.makeText(this, "Excel file saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e("TableOCRCompletionActivity", "Error saving Excel file", e)
            Toast.makeText(this, "Failed to save Excel file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
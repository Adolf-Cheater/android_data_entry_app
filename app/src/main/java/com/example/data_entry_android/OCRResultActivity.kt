package com.example.data_entry_android

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.data_entry_android.databinding.ActivityOcrResultBinding
import org.json.JSONObject
import android.os.Build
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import android.Manifest
import android.graphics.Bitmap
import androidx.activity.result.contract.ActivityResultContracts
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.ParagraphAlignment
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder
import java.math.BigInteger


class OCRResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOcrResultBinding
    private var ocrResultBitmap: Bitmap? = null


    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                saveImage()
            } else {
                Toast.makeText(this, "Permission denied to save image", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOcrResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val ocrResponse = intent.getStringExtra("OCR_RESPONSE")

        Log.d("OCRResultActivity", "Received image URI: $imageUriString")

        if (imageUriString == null || ocrResponse == null) {
            Toast.makeText(this, "Error: Missing image or OCR data", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val imageUri = Uri.parse(imageUriString)

        try {
            displayOriginalImage(imageUri)
            displayOCRResult(imageUri, ocrResponse)
        } catch (e: Exception) {
            Log.e("OCRResultActivity", "Error displaying image or OCR result", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
        binding.saveImageButton.setOnClickListener {
            checkPermissionAndSaveImage()
        }
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.generateDocxButton.setOnClickListener {
            generateDocxFile()
        }
    }

    private fun generateDocxFile() {
        val ocrResponse = intent.getStringExtra("OCR_RESPONSE")
        if (ocrResponse == null) {
            Toast.makeText(this, "Error: Missing OCR data", Toast.LENGTH_LONG).show()
            return
        }

        val ocrData = parseOCRResponse(ocrResponse)

        try {
            val document = XWPFDocument()

            // Create a table to represent the image
            val table = document.createTable(100, 100) // Adjust these numbers based on desired granularity

            // Set minimal cell margins
            val tableCellProperties = table.ctTbl.tblPr.addNewTblCellMar()
            tableCellProperties.addNewTop().w = BigInteger.ZERO
            tableCellProperties.addNewBottom().w = BigInteger.ZERO
            tableCellProperties.addNewLeft().w = BigInteger.ZERO
            tableCellProperties.addNewRight().w = BigInteger.ZERO

            // Remove borders
            val borders = table.ctTbl.tblPr.addNewTblBorders()
            borders.addNewTop().setVal(STBorder.NONE)
            borders.addNewBottom().setVal(STBorder.NONE)
            borders.addNewLeft().setVal(STBorder.NONE)
            borders.addNewRight().setVal(STBorder.NONE)
            borders.addNewInsideH().setVal(STBorder.NONE)
            borders.addNewInsideV().setVal(STBorder.NONE)

            // Calculate scaling factors
            val maxX = ocrData.maxOf { it.boundingBox.right }
            val maxY = ocrData.maxOf { it.boundingBox.bottom }
            val scaleX = 100.0 / maxX
            val scaleY = 100.0 / maxY

            ocrData.forEach { item ->
                val startRow = (item.boundingBox.top * scaleY).toInt()
                val startCol = (item.boundingBox.left * scaleX).toInt()
                val endRow = (item.boundingBox.bottom * scaleY).toInt()
                val endCol = (item.boundingBox.right * scaleX).toInt()

                for (row in startRow..endRow) {
                    for (col in startCol..endCol) {
                        if (row < 100 && col < 100) {  // Ensure we're within table bounds
                            val cell = table.getRow(row).getCell(col)
                            cell.text = item.word
                            cell.paragraphs[0].alignment = ParagraphAlignment.CENTER

                            // Set font size (adjust as needed)
                            val run = cell.paragraphs[0].createRun()
                            run.fontSize = 8
                        }
                    }
                }
            }

            // Save the document
            val fileName = "OCR_Result_${System.currentTimeMillis()}.docx"
            val file = File(getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { out ->
                document.write(out)
            }

            Toast.makeText(this, "Docx file generated: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("OCRResultActivity", "Error generating docx file", e)
            Toast.makeText(this, "Failed to generate docx file: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionAndSaveImage() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // Android 10 and above
                saveImage()
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                // Android 6.0 to Android 9.0
                when {
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        saveImage()
                    }

                    else -> {
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
            }

            else -> {
                // Below Android 6.0
                saveImage()
            }
        }
    }

    private fun displayOriginalImage(imageUri: Uri) {
        Log.d("OCRResultActivity", "Displaying original image: $imageUri")
        binding.originalImageView.setImageURI(imageUri)
    }


    private fun displayOCRResult(imageUri: Uri, ocrResponse: String) {
        try {
            val inputStream = contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutableBitmap)

            val paint = Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }

            val textPaint = Paint().apply {
                color = Color.RED
                textSize = 24f
            }

            val ocrData = parseOCRResponse(ocrResponse)

            for (item in ocrData) {
                val rect = item.boundingBox
                canvas.drawRect(rect, paint)
                canvas.drawText(item.word, rect.left, rect.top - 5, textPaint)
            }

            // Store the result bitmap
            ocrResultBitmap = mutableBitmap

            // Display the OCR result image
            binding.ocrResultImageView.setImageBitmap(mutableBitmap)

            Log.d("OCRResultActivity", "OCR result image set to ImageView")
        } catch (e: Exception) {
            Log.e("OCRResultActivity", "Error processing image for OCR result", e)
            Toast.makeText(this, "Error processing image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    private fun saveImage() {
        ocrResultBitmap?.let { bitmap ->
            val filename = "OCR_Result_${System.currentTimeMillis()}.jpg"
            var fos: OutputStream? = null
            var imageUri: Uri? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    contentResolver.also { resolver ->
                        imageUri = resolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                        )
                        fos = imageUri?.let { resolver.openOutputStream(it) }
                    }
                } else {
                    val imagesDir =
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val image = File(imagesDir, filename)
                    fos = FileOutputStream(image)
                    imageUri = Uri.fromFile(image)
                }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                    Toast.makeText(this, "Image saved successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OCRResultActivity", "Error saving image", e)
                Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
        }
    }


    private fun parseOCRResponse(response: String): List<OCRItem> {
        val jsonObject = JSONObject(response)
        val wordsInfo = jsonObject.getJSONArray("prism_wordsInfo")
        val ocrItems = mutableListOf<OCRItem>()

        for (i in 0 until wordsInfo.length()) {
            val wordInfo = wordsInfo.getJSONObject(i)
            val word = wordInfo.getString("word")
            val pos = wordInfo.getJSONArray("pos")

            val left = pos.getJSONObject(0).getInt("x")
            val top = pos.getJSONObject(0).getInt("y")
            val right = pos.getJSONObject(2).getInt("x")
            val bottom = pos.getJSONObject(2).getInt("y")

            ocrItems.add(
                OCRItem(
                    word,
                    RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
                )
            )
        }

        return ocrItems
    }

    data class OCRItem(val word: String, val boundingBox: RectF)
}
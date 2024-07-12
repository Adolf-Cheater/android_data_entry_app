package com.example.data_entry_android


import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.data_entry_android.databinding.ActivityMainBinding
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.Call
import retrofit2.Response
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.ExperimentalEncodingApi
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.os.Build
import android.widget.CheckBox
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.Header
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.text.SimpleDateFormat

// Only use for future implmentations
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import android.graphics.Rect
import android.view.View
import kotlinx.coroutines.CoroutineStart
import retrofit2.Callback



class MainActivity<IOException : Any> : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var selectedImageUri: Uri? = null
    private lateinit var currentPhotoPath: String

    private val PERMISSION_REQUEST_CODE = 200
    private val IMAGE_CAPTURE_CODE = 1001
    private val CROP_REQUEST_CODE = 1002


    /*#####################################
    API URL ENTRY AREA - SUBJECT TO CHANGE
    ######################################*/
    private val TABLE_OCR_URL = "https://form.market.alicloudapi.com/api/predict/ocr_table_parse"
    private val TABLE_OCR_APPCODE = "c98f40b96d014578b8f793970e8a002c"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.saveCropButton.setOnClickListener {
            saveCroppedImage()
        }

        binding.cancelCropButton.setOnClickListener {
            cancelCropping()
        }
        binding.selectImageButton.setOnClickListener {
            pickImage.launch("image/*")
        }

        binding.takePictureButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }

        binding.tableOcrButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                showTableOCRConfigDialog(uri)
            } ?: run {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.ocrButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                performOCR(uri)
            } ?: run {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tableOcrButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                showTableOCRConfigDialog(uri)
            } ?: run {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.slideToActionView.setSlideListener {
            // This is called when the slider is fully slid
            val intent = Intent(this, ContentActivity::class.java)
            startActivity(intent)
            finish()
        }

        //regular OCR button - do not use for now
        /*
        binding.ocrButton.setOnClickListener {
            performOCR()
        }
        */

        /*
        binding.sliderSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val maxProgress = seekBar?.max ?: 100
                if (progress >= maxProgress * 0.8) {
                    // Start the new activity
                    val intent = Intent(this@MainActivity, ContentActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

         */
    }

    private fun setupGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.cropView.setOnApplyWindowInsetsListener { v, insets ->
                val displayCutout = insets.displayCutout
                if (displayCutout != null) {
                    val exclusionRects = mutableListOf<android.graphics.Rect>()

                    // Left edge
                    exclusionRects.add(android.graphics.Rect(0, 0, 100, v.height))
                    // Right edge
                    exclusionRects.add(android.graphics.Rect(v.width - 100, 0, v.width, v.height))
                    // Top edge
                    exclusionRects.add(android.graphics.Rect(0, 0, v.width, 100))
                    // Bottom edge
                    exclusionRects.add(android.graphics.Rect(0, v.height - 100, v.width, v.height))

                    v.systemGestureExclusionRects = exclusionRects
                }
                insets
            }
        }
    }

    private fun cropImageWithAndroid(sourceUri: Uri) {
        val destinationUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )

        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(sourceUri, "image/*")
            putExtra("crop", "true")
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra("outputX", 300)
            putExtra("outputY", 300)
            putExtra("scale", true)
            putExtra(MediaStore.EXTRA_OUTPUT, destinationUri)
            putExtra("return-data", false)
            putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        grantUriPermission(cropIntent.resolveActivity(packageManager).packageName, sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        grantUriPermission(cropIntent.resolveActivity(packageManager).packageName, destinationUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)

        startActivityForResult(cropIntent, CROP_REQUEST_CODE)
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: Exception) {
                    Log.e("MainActivity", "Error creating image file", ex)
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, IMAGE_CAPTURE_CODE)
                }
            }
        }
    }

    private fun showCustomCropView(imageUri: Uri) {
        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        binding.cropView.setBitmap(bitmap)
        binding.cropLayout.setVisibility(android.view.View.VISIBLE)
        binding.mainLayout.setVisibility(android.view.View.GONE)
        setupGestureExclusion()
    }

    private fun showCropMethodDialog(imageUri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Choose Cropping Method")
            .setMessage("Which cropping method would you like to use?")
            .setPositiveButton("Built-in Android") { _, _ ->
                cropImageWithAndroid(imageUri)
            }
            .setNegativeButton("Custom Crop") { _, _ ->
                showCustomCropView(imageUri)
            }
            .show()
    }

    private fun saveCroppedImage() {
        val croppedBitmap = binding.cropView.getCroppedBitmap()
        croppedBitmap?.let { bitmap ->
            val file = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }
            selectedImageUri = Uri.fromFile(file)
            binding.selectedImageView.setImageURI(selectedImageUri)
            binding.selectedImageView.setImageBitmap(bitmap)
            hideCropView()
        }
    }

    private fun cancelCropping() {
        hideCropView()
    }

    private fun hideCropView() {
        binding.cropLayout.setVisibility(android.view.View.GONE)
        binding.mainLayout.setVisibility(android.view.View.VISIBLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.cropView.systemGestureExclusionRects = emptyList()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d("MainActivity", "onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                IMAGE_CAPTURE_CODE -> {
                    Log.d("MainActivity", "Image captured, showing crop method dialog")
                    val imageUri = FileProvider.getUriForFile(
                        this,
                        "${packageName}.fileprovider",
                        File(currentPhotoPath)
                    )
                    showCropMethodDialog(imageUri)
                }
                CROP_REQUEST_CODE -> {
                    Log.d("MainActivity", "Crop completed, handling result")
                    handleCropResult(data)
                }
            }
        } else {
            Log.e("MainActivity", "Activity result not OK: requestCode=$requestCode, resultCode=$resultCode")
            Toast.makeText(this, "Operation cancelled or failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cropImage(sourceUri: Uri) {
        Log.d("MainActivity", "Starting crop with sourceUri: $sourceUri")
        val destinationUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        )
        Log.d("MainActivity", "Destination URI: $destinationUri")

        val cropIntent = Intent("com.android.camera.action.CROP").apply {
            setDataAndType(sourceUri, "image/*")
            putExtra("crop", "true")
            putExtra("aspectX", 1)
            putExtra("aspectY", 1)
            putExtra("outputX", 300)
            putExtra("outputY", 300)
            putExtra("scale", true)
            putExtra(MediaStore.EXTRA_OUTPUT, destinationUri)
            putExtra("return-data", false)
            putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        // Grant permissions to all apps that can handle the crop intent
        val resInfoList = packageManager.queryIntentActivities(cropIntent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            grantUriPermission(packageName, sourceUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            grantUriPermission(packageName, destinationUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            Log.d("MainActivity", "Starting crop activity")
            startActivityForResult(cropIntent, CROP_REQUEST_CODE)
        } catch (e: ActivityNotFoundException) {
            Log.e("MainActivity", "Crop activity not found", e)
            Toast.makeText(this, "Your device doesn't support image cropping", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting crop activity", e)
            Toast.makeText(this, "Failed to start cropping: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCropResult(data: Intent?) {
        Log.d("MainActivity", "Handling crop result")

        if (data == null) {
            Log.e("MainActivity", "Crop result data is null")
            Toast.makeText(this, "Failed to crop image: No result data", Toast.LENGTH_SHORT).show()
            return
        }

        var uri = data.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT)
        if (uri == null) {
            uri = data.data
            Log.d("MainActivity", "URI from data: $uri")
        }

        if (uri == null) {
            Log.e("MainActivity", "Crop result URI is null")
            Toast.makeText(this, "Failed to crop image: No result URI", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("MainActivity", "Crop result URI: $uri")
        try {
            // Copy the cropped image to a new file in our app's private directory
            val inputStream = contentResolver.openInputStream(uri)
            val outputFile = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "cropped_image_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(outputFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            // Use the new file URI
            val newUri = Uri.fromFile(outputFile)
            selectedImageUri = newUri
            binding.selectedImageView.setImageURI(selectedImageUri)
            Log.d("MainActivity", "Successfully set cropped image")
            Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error copying cropped image", e)
            Toast.makeText(this, "Failed to process cropped image: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error handling crop result", e)
            Toast.makeText(this, "Failed to process cropped image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun compressImage(imageUri: Uri) {
        val file = File(imageUri.path!!)
        if (file.length() > 10 * 1024 * 1024) { // If file size is larger than 10MB
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            var compressQuality = 100
            var streamLength: Int
            val bmpStream = ByteArrayOutputStream()
            do {
                bmpStream.reset()
                bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
                val bmpPicByteArray = bmpStream.toByteArray()
                streamLength = bmpPicByteArray.size
                compressQuality -= 5
            } while (streamLength > 10 * 1024 * 1024 && compressQuality > 5)

            FileOutputStream(file).use { fo ->
                fo.write(bmpStream.toByteArray())
            }
        }
    }


    private fun createImageFile(): File? {
        return try {
            val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(
                "JPEG_${timeStamp}_",
                ".jpg",
                storageDir
            ).apply {
                currentPhotoPath = absolutePath
            }
        } catch (ex: Exception) {
            // Log the error
            Log.e("MainActivity", "Error creating image file", ex)
            null
        }
    }


    //REMEMBER TO FIX
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Optionally, you can display the selected image in an ImageView
            binding.selectedImageView.setImageURI(selectedImageUri)
            Log.d("MainActivity", "Selected image URI: $selectedImageUri")
        }

    }
    private fun showTableOCRConfigDialog(imageUri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_table_ocr_config, null)
        val checkBoxDirAssure = dialogView.findViewById<CheckBox>(R.id.checkBoxDirAssure)
        val checkBoxLineLess = dialogView.findViewById<CheckBox>(R.id.checkBoxLineLess)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Proceed") { _, _ ->
                val dirAssure = checkBoxDirAssure.isChecked
                val lineLess = checkBoxLineLess.isChecked
                performTableOCR(imageUri, dirAssure, lineLess)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performTableOCR(imageUri: Uri, dirAssure: Boolean, lineLess: Boolean) {

        Log.d("MainActivity", "Performing OCR on image: $imageUri")
        val intent = Intent(this, ProcessingActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Image = getBase64FromUri(imageUri)

                val client = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://form.market.alicloudapi.com/")
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create())
                    .build()

                val service = retrofit.create(TableOCRInterface::class.java)

                val params = JSONObject().apply {
                    put("image", base64Image)
                    put("configure", JSONObject().apply {
                        put("format", "xlsx")
                        put("dir_assure", dirAssure.toString())
                        put("line_less", (!lineLess).toString())
                    })
                }

                val requestBody = params.toString().toRequestBody("application/json".toMediaTypeOrNull())
                val authorization = "APPCODE $TABLE_OCR_APPCODE"

                val response = service.getTableOCRResults(authorization, requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        responseBody?.let {
                            val jsonResponse = JSONObject(it.charStream().readText())
                            if (jsonResponse.has("tables")) {
                                val base64Excel = jsonResponse.getString("tables")

                                // Start TableOCRCompletionActivity
                                val intent = Intent(this@MainActivity, TableOCRCompletionActivity::class.java)
                                intent.putExtra("IMAGE_URI", imageUri.toString())
                                intent.putExtra("OCR_RESPONSE", base64Excel)
                                startActivity(intent)
                            } else {
                                showErrorAndReturnToMain("Unexpected response format")
                            }
                        }
                    } else {
                        showErrorAndReturnToMain("Table OCR request failed")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorAndReturnToMain("Network error: ${e.message}")
                }
            }
        }
    }

    private fun showErrorAndReturnToMain(message: String) {
        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
        // Return to MainActivity
        val intent = Intent(this@MainActivity, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    private fun saveExcelFile(base64Excel: String) {
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
    }


    interface TableOCRInterface {
        @POST("api/predict/ocr_table_parse")
        suspend fun getTableOCRResults(
            @Header("Authorization") authorization: String,
            @Body requestBody: RequestBody
        ): Response<ResponseBody>
    }

    private fun performOCR(imageUri: Uri) {
        Log.d("MainActivity", "Performing OCR on image: $imageUri")

        // Start ProcessingActivity
        val processingIntent = Intent(this, ProcessingActivity::class.java)
        processingIntent.putExtra("IMAGE_URI", imageUri.toString())
        startActivity(processingIntent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Image = getBase64FromUri(imageUri)

                val client = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .build()

                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()

                val retrofit = Retrofit.Builder()
                    .baseUrl("https://gjbsb.market.alicloudapi.com/")
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

                val service = retrofit.create(OCRInterface::class.java)

                val params = JSONObject().apply {
                    put("img", base64Image)
                    put("prob", false)
                    put("charInfo", false)
                    put("rotate", false)
                    put("table", true)
                    put("sortPage", true)
                    put("noStamp", false)
                    put("figure", false)
                    put("row", true)
                    put("paragraph", false)
                    put("oricoord", true)
                }

                val requestBody = params.toString().toRequestBody("application/json".toMediaType())

                val appcode = "c98f40b96d014578b8f793970e8a002c"
                val authorization = "APPCODE $appcode"

                val response = service.getOCRResults(authorization, requestBody)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val ocrResponse = response.body()
                        ocrResponse?.let {
                            Log.d("MainActivity", "OCR successful, starting CompletionActivity")
                            val completionIntent = Intent(this@MainActivity, CompletionActivity::class.java).apply {
                                putExtra("IMAGE_URI", imageUri.toString())
                                putExtra("OCR_RESPONSE", Gson().toJson(it))
                            }
                            Log.d("MainActivity", "Sending IMAGE_URI: ${imageUri.toString()}")
                            Log.d("MainActivity", "Sending OCR_RESPONSE: ${Gson().toJson(it)}")
                            startActivity(completionIntent)
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                        } ?: run {
                            showErrorAndReturnToMain("OCR response is null")
                        }
                    } else {
                        showErrorAndReturnToMain("OCR request failed: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorAndReturnToMain("Network error: ${e.message}")
                }
            }
        }
    }

    // Update OCRInterface to use suspend function
    interface OCRInterface {
        @Headers("Content-Type: application/json")
        @POST("ocrservice/advanced")
        suspend fun getOCRResults(
            @Header("Authorization") authorization: String,
            @Body requestBody: RequestBody
        ): Response<OCRResponse>
    }
    @OptIn(ExperimentalEncodingApi::class)

    /*
    private fun getBase64FromResourceImage(resourceId: Int): String {
        val inputStream = resources.openRawResource(resourceId)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
     */
    private fun getBase64FromUri(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }
}


interface OCRService {
    @Headers("Content-Type: application/json")
    @POST("ocrservice/advanced")
    fun recognizeText(@Body request: OCRRequest): Call<OCRResponse>
}

data class OCRRequest(
    val headers: Map<String, String>,
    val params: Map<String, Any>
)
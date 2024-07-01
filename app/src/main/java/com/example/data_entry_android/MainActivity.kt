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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
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
import retrofit2.Callback
import retrofit2.Response
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineStart
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
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.text.SimpleDateFormat




class MainActivity<IOException : Any> : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selectedImageUri: Uri? = null
    private lateinit var currentPhotoPath: String

    private val PERMISSION_REQUEST_CODE = 200
    private val IMAGE_CAPTURE_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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




        binding.ocrButton.setOnClickListener {
            selectedImageUri?.let { uri ->
                performOCR(uri)
            } ?: run {
                Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show()
            }
        }

        /*
        binding.ocrButton.setOnClickListener {
            performOCR()
        }
        */

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_CAPTURE_CODE && resultCode == RESULT_OK) {
            selectedImageUri = Uri.fromFile(File(currentPhotoPath))
            compressImage(selectedImageUri!!)
            binding.selectedImageView.setImageURI(selectedImageUri)
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

    private fun performOCR(imageUri: Uri) {
        Log.d("MainActivity", "Performing OCR on image: $imageUri")
        val base64Image = getBase64FromUri(imageUri)
        //val base64Image = getBase64FromResourceImage(R.drawable.test2)


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
            put("table", false)
            put("sortPage", false)
            put("noStamp", false)
            put("figure", false)
            put("row", false)
            put("paragraph", false)
            put("oricoord", true)
        }

        val requestBody = params.toString().toRequestBody("application/json".toMediaType())

        val appcode = "c98f40b96d014578b8f793970e8a002c"
        val authorization = "APPCODE $appcode"

        val call = service.getOCRResults(authorization, requestBody)

        call.enqueue(object : Callback<OCRResponse> {
            override fun onResponse(call: Call<OCRResponse>, response: Response<OCRResponse>) {
                if (response.isSuccessful) {
                    val ocrResponse = response.body()
                    ocrResponse?.let {
                        Log.d("MainActivity", "Sending image to OCRResultActivity: $imageUri")
                        val intent = Intent(this@MainActivity, OCRResultActivity::class.java).apply {
                            putExtra("IMAGE_URI", imageUri.toString())
                            putExtra("OCR_RESPONSE", Gson().toJson(it))
                        }
                        startActivity(intent)
                    }
                } else {
                    // Handle error
                    Toast.makeText(this@MainActivity, "OCR request failed", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<OCRResponse>, t: Throwable) {
                // Handle failure
                Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
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
// OCRInterface.kt
package com.example.data_entry_android

import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface OCRInterface {
    @Headers("Content-Type: application/json; charset=UTF-8")
    @POST("ocrservice/advanced")
    fun getOCRResults(
        @Header("Authorization") authorization: String,
        @Body requestBody: RequestBody
    ): Call<OCRResponse>
}
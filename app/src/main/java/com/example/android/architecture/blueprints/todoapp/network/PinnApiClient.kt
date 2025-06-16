package com.example.android.architecture.blueprints.todoapp.network

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Data class for the PINN API response
 */
data class PinnResponse(
    val status: String,
    val output: PinnOutput? = null
)

/**
 * Data class for the optimized output from the PINN model
 */
data class PinnOutput(
    val optimizedDepth: Double,
    val optimizedRadius: Double,
    val optimizedConductivity: Double
)

/**
 * API client for interacting with the PINN FastAPI endpoint
 */
object PinnApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private const val API_URL = "http://10.203.21.195:8000/predict"

    /**
     * Sends a request to the PINN model with the given parameters and files
     * @param conductivity The conductivity value
     * @param radius The radius value
     * @param depth The depth value
     * @param geometryFile The geometry file
     * @param surfaceTempFile The surface temperature file
     * @param callback Callback to handle the response or error
     */
    fun predictTask(
        conductivity: Double,
        radius: Double,
        depth: Double,
        geometryFile: File,
        surfaceTempFile: File,
        callback: (PinnResponse?, String?) -> Unit
    ) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("conductivity", conductivity.toString())
            .addFormDataPart("radius", radius.toString())
            .addFormDataPart("depth", depth.toString())
            .addFormDataPart(
                "geometry_file",
                geometryFile.name,
                geometryFile.asRequestBody("text/plain".toMediaTypeOrNull())
            )
            .addFormDataPart(
                "surface_temp_file",
                surfaceTempFile.name,
                surfaceTempFile.asRequestBody("text/plain".toMediaTypeOrNull())
            )
            .build()

        val request = okhttp3.Request.Builder()
            .url(API_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                callback(null, "Network error: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val prediction = kotlinx.serialization.json.Json.decodeFromString<PinnResponse>(responseBody)
                    callback(prediction, null)
                } else {
                    callback(null, "API error: ${response.code} - ${response.message}")
                }
            }
        })
    }
}
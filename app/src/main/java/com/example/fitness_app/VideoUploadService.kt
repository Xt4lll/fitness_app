package com.example.fitness_app

import android.content.Context
import android.net.Uri
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import com.example.fitness_app.ui.VideoUploadConfig

class VideoUploadService(private val context: Context) {
    private val client = OkHttpClient()

    fun uploadVideo(uri: Uri, userId: String): Result<String> = try {
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(uri)
        val videoBytes = inputStream?.readBytes() ?: throw IOException("Failed to read video file")

        val filename = "video_${System.currentTimeMillis()}_$userId"
        val videoRequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", filename,
                videoBytes.toRequestBody("video/*".toMediaTypeOrNull())
            )
            .addFormDataPart("fileName", filename)
            .addFormDataPart("useUniqueFilename", "true")
            .addFormDataPart("folder", VideoUploadConfig.VIDEO_FOLDER)
            .build()

        val credential = Credentials.basic(VideoUploadConfig.PRIVATE_API_KEY, "")
        val videoRequest = Request.Builder()
            .url(VideoUploadConfig.UPLOAD_URL)
            .addHeader("Authorization", credential)
            .post(videoRequestBody)
            .build()

        val response = client.newCall(videoRequest).execute()
        if (!response.isSuccessful) {
            throw IOException("Upload failed with code: ${response.code}")
        }

        val bodyString = response.body?.string()
        val json = JSONObject(bodyString ?: "")
        val videoUrl = json.optString("url", null) ?: throw IOException("No video URL in response")

        Result.success(videoUrl)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
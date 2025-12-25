package com.readapp.data

import com.readapp.data.model.Book
import com.readapp.data.model.Chapter
import com.readapp.data.model.HttpTTS
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.Response
import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ReadRepository(private val apiFactory: (String) -> ReadApiService) {

    suspend fun login(baseUrl: String, publicUrl: String?, username: String, password: String): Result<BookLoginResult> =
        executeWithFailover<BookLoginResult> { api ->
            api.login(username, password)
        }(buildEndpoints(baseUrl, publicUrl))

    suspend fun fetchBooks(baseUrl: String, publicUrl: String?, accessToken: String): Result<List<Book>> =
        executeWithFailover {
            it.getBookshelf(accessToken)
        }(buildEndpoints(baseUrl, publicUrl)).map { list -> list.map { book -> book.toUiModel() } }

    suspend fun fetchChapterList(
        baseUrl: String,
        publicUrl: String?,
        accessToken: String,
        bookUrl: String,
        bookSourceUrl: String?,
    ): Result<List<Chapter>> = executeWithFailover {
        it.getChapterList(accessToken, bookUrl, bookSourceUrl)
    }(buildEndpoints(baseUrl, publicUrl))

    suspend fun fetchChapterContent(
        baseUrl: String,
        publicUrl: String?,
        accessToken: String,
        bookUrl: String,
        bookSourceUrl: String?,
        index: Int,
    ): Result<String> = executeWithFailover {
        it.getBookContent(accessToken, bookUrl, index, 0, bookSourceUrl)
    }(buildEndpoints(baseUrl, publicUrl))

    suspend fun saveBookProgress(
        baseUrl: String,
        publicUrl: String?,
        accessToken: String,
        bookUrl: String,
        index: Int,
        pos: Double,
        title: String?,
    ): Result<String> = executeWithFailover {
        it.saveBookProgress(accessToken, bookUrl, index, pos, title)
    }(buildEndpoints(baseUrl, publicUrl))

    suspend fun fetchDefaultTts(baseUrl: String, publicUrl: String?, accessToken: String): Result<String> =
        executeWithFailover { it.getDefaultTts(accessToken) }(buildEndpoints(baseUrl, publicUrl))

    suspend fun fetchTtsEngines(baseUrl: String, publicUrl: String?, accessToken: String): Result<List<HttpTTS>> =
        executeWithFailover { it.getAllTts(accessToken) }(buildEndpoints(baseUrl, publicUrl))

    suspend fun importBook(
        baseUrl: String,
        publicUrl: String?,
        accessToken: String,
        fileUri: Uri,
        context: Context
    ): Result<Any> {
        val filePart = createMultipartBodyPart(fileUri, context)
            ?: return Result.failure(IllegalArgumentException("无法创建文件部分"))

        return executeWithFailover {
            it.importBook(accessToken, filePart)
        }(buildEndpoints(baseUrl, publicUrl))
    }

    private fun createMultipartBodyPart(fileUri: Uri, context: Context): MultipartBody.Part? {
        return context.contentResolver.openInputStream(fileUri)?.use { inputStream ->
            val fileBytes = inputStream.readBytes()
            val requestFile = fileBytes.toRequestBody(
                context.contentResolver.getType(fileUri)?.toMediaTypeOrNull()
            )
            MultipartBody.Part.createFormData(
                "file",
                getFileName(fileUri, context),
                requestFile
            )
        }
    }

    private fun getFileName(uri: Uri, context: Context): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (columnIndex >= 0) {
                        result = cursor.getString(columnIndex)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != -1) {
                if (cut != null) {
                    result = result?.substring(cut + 1)
                }
            }
        }
        return result
    }

    fun buildTtsAudioUrl(baseUrl: String, accessToken: String, ttsId: String, text: String, speechRate: Double): String? {
        val normalized = ensureTrailingSlash(baseUrl)
        return "${normalized}tts".toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("accessToken", accessToken)
            ?.addQueryParameter("id", ttsId)
            ?.addQueryParameter("speakText", text)
            ?.addQueryParameter("speechRate", speechRate.toString())
            ?.build()
            ?.toString()
    }

    private fun buildEndpoints(primary: String, secondary: String?): List<String> {
        val endpoints = mutableListOf<String>()
        if (primary.isNotBlank()) {
            endpoints.add(ensureTrailingSlash(primary))
        }
        if (!secondary.isNullOrBlank() && secondary != primary) {
            endpoints.add(ensureTrailingSlash(secondary))
        }
        return endpoints
    }

    private fun ensureTrailingSlash(url: String): String = if (url.endsWith('/')) url else "$url/"

    private fun <T> executeWithFailover(block: suspend (ReadApiService) -> Response<com.readapp.data.model.ApiResponse<T>>):
        suspend (List<String>) -> Result<T> = lambda@ { endpoints: List<String> ->
        var lastError: Throwable? = null
        for (endpoint in endpoints) {
            val api = apiFactory(endpoint)
            try {
                val response = block(api)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.isSuccess && body.data != null) {
                        return@lambda Result.success(body.data)
                    }
                    lastError = IllegalStateException(body?.errorMsg ?: "未知错误")
                } else {
                    lastError = IllegalStateException("服务器返回状态码 ${response.code()}")
                }
            } catch (e: Exception) {
                lastError = e
            }
        }
        Result.failure(lastError ?: IllegalStateException("未知错误"))
    }
}

private typealias BookLoginResult = com.readapp.data.model.LoginResponse

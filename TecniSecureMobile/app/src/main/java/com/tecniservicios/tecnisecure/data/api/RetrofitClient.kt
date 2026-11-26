package com.tecniservicios.tecnisecure.data.api

import android.content.Context
import com.tecniservicios.tecnisecure.data.local.SessionManager
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private var apiService: ApiService? = null
    private var currentBaseUrl: String? = null

    fun getApiService(context: Context): ApiService {
        val appContext = context.applicationContext
        val sessionManager = SessionManager(appContext)
        val baseUrl = sessionManager.getBaseUrl()

        if (apiService == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl

            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val tokenAuthenticator = Authenticator { _, response ->
                val path = response.request.url.encodedPath
                if (path.contains("/api/auth/") || responseCount(response) >= 2) {
                    return@Authenticator null
                }
                val refreshToken = sessionManager.getRefreshToken() ?: return@Authenticator null

                try {
                    val refreshClient = OkHttpClient.Builder()
                        .connectTimeout(20, TimeUnit.SECONDS)
                        .readTimeout(20, TimeUnit.SECONDS)
                        .build()

                    val payload = JSONObject().put("refreshToken", refreshToken).toString()
                    val refreshReq = Request.Builder()
                        .url("${baseUrl.trimEnd('/')}/api/auth/refresh")
                        .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .header("Accept", "application/json")
                        .build()

                    val refreshRes = refreshClient.newCall(refreshReq).execute()
                    if (refreshRes.isSuccessful) {
                        val bodyStr = refreshRes.body?.string()
                        if (!bodyStr.isNullOrBlank()) {
                            val json = JSONObject(bodyStr)
                            val newAccess = json.optString("accessToken")
                            val newRefresh = json.optString("refreshToken", refreshToken)
                            if (newAccess.isNotBlank()) {
                                sessionManager.saveTokens(newAccess, newRefresh)
                                return@Authenticator response.request.newBuilder()
                                    .header("Authorization", "Bearer $newAccess")
                                    .build()
                            }
                        }
                    }
                } catch (_: Exception) {
                }
                null
            }

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(sessionManager))
                .authenticator(tokenAuthenticator)
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiService::class.java)
        }

        return apiService!!
    }

    private fun responseCount(response: okhttp3.Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}

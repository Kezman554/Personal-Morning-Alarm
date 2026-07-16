package com.personalmorningalarm.data.remote

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the [AlfredApiService] against the address saved in [AlfredSettings],
 * rebuilding only when that address changes.
 *
 * Timeouts are deliberately short: Alfred lives on the home network, so it either
 * answers fast or it isn't there. An unreachable Alfred must never leave a content
 * screen hanging mid-morning — callers fall back to cache well inside the screen's
 * dismiss timer.
 */
object AlfredClient {

    private const val TIMEOUT_SECONDS = 3L

    private val http = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private var cachedBaseUrl: String? = null
    private var cachedService: AlfredApiService? = null

    @Synchronized
    fun service(settings: AlfredSettings): AlfredApiService {
        val baseUrl = settings.baseUrl()
        cachedService?.let { if (baseUrl == cachedBaseUrl) return it }
        val service = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AlfredApiService::class.java)
        cachedBaseUrl = baseUrl
        cachedService = service
        return service
    }
}

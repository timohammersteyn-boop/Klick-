package com.example.splice

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Splice API Retrofit service.
 * Connects directly to the secure app backend proxy which holds the Splice credentials server-side,
 * ensuring no API keys or secret tokens are bundled inside the Android APK.
 */
interface SpliceApiService {

    @GET("samples/search")
    suspend fun searchSamples(
        @Query("query") query: String,
        @Query("category") category: String? = null,
        @Query("bpm") bpm: Int? = null,
        @Query("limit") limit: Int = 30
    ): SpliceSearchResponse

    @GET("samples/catalog")
    suspend fun getFeaturedCatalog(
        @Query("limit") limit: Int = 30
    ): SpliceSearchResponse

    companion object {
        // App backend proxy endpoint (API keys managed server-side)
        private const val BACKEND_BASE_URL = "https://api.schwunglive.com/api/"

        fun create(): SpliceApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("X-App-Client", "SchwungLive-Android/1.0")
                        .build()
                    chain.proceed(request)
                }
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BACKEND_BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(SpliceApiService::class.java)
        }
    }
}

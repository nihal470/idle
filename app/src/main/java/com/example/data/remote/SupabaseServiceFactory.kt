package com.example.data.remote

import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    val projectName: String = BuildConfig.SUPABASE_PROJECT_NAME
    val projectId: String = BuildConfig.SUPABASE_PROJECT_ID
    val projectUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')
    val publishableKey: String = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim()

    val isApiKeyConfigured: Boolean
        get() = publishableKey.isNotBlank() &&
            !publishableKey.equals("YOUR_SUPABASE_PUBLISHABLE_KEY", ignoreCase = true) &&
            !publishableKey.startsWith("MY_", ignoreCase = true)
}

object SupabaseServiceFactory {

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val key = SupabaseConfig.publishableKey
            val requestBuilder = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            if (key.isNotBlank()) {
                requestBuilder
                    .header("apikey", key)
                    .header("Authorization", "Bearer $key")
            }
            chain.proceed(requestBuilder.build())
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val okHttpClient: OkHttpClient by lazy { buildOkHttpClient() }

    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl("${SupabaseConfig.projectUrl}/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SupabaseApi::class.java)
    }

    fun createRealtimeClient(): SupabaseRealtimeClient {
        return SupabaseRealtimeClient(
            okHttpClient = okHttpClient,
            baseUrl = SupabaseConfig.projectUrl,
            apiKey = if (SupabaseConfig.isApiKeyConfigured) SupabaseConfig.publishableKey else "",
        )
    }
}

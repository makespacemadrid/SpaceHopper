package com.mks.hackerspaces

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface SpaceApiService {
    @GET("https://directory.spaceapi.io/")
    suspend fun getDirectory(): Map<String, String>

    @GET
    suspend fun getSpace(@Url url: String): SpaceApi
}

object RetrofitInstance {
    private var apiInstance: SpaceApiService? = null
    private var currentUnsafeState: Boolean? = null

    private fun getClient(unsafe: Boolean): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)

        if (unsafe) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("SSL")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())

                builder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                builder.hostnameVerifier { _, _ -> true }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        
        return builder.build()
    }

    fun getApi(unsafe: Boolean): SpaceApiService {
        if (apiInstance == null || currentUnsafeState != unsafe) {
            apiInstance = Retrofit.Builder()
                .baseUrl("https://directory.spaceapi.io/")
                .client(getClient(unsafe))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SpaceApiService::class.java)
            currentUnsafeState = unsafe
        }
        return apiInstance!!
    }
    
    // For backwards compatibility with initial code if needed, though we should migrate calls
    val api: SpaceApiService get() = getApi(true) 

    fun reset() {
        apiInstance = null
    }
}

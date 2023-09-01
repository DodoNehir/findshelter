package com.example.findshelter

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okio.ByteString
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url
import java.io.IOException
import java.net.URI
import java.net.URL

interface AreaCodeService {
    @GET
    fun getAreaCode(
        @Url url: String
    ): Call<AreaCodeResponse>

    @GET("getStanReginCdList")
//    @Headers("Accept-Encoding: identity")
//    @Headers("Content-Type: application/json; charset=utf-8")
    fun getAreaCode(
        @Query("ServiceKey", encoded = true) key: String,
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 3,
        @Query("type") type: String = "xml",
        @Query("locatadd_nm", encoded = false) locatadd_nm: String
    ): Call<AreaCodeResponse>

    companion object {
        private const val BASE_URL = "https://apis.data.go.kr/1741000/StanReginCd/"

        fun create(): AreaCodeService {
            val gson: Gson = GsonBuilder().setLenient().create()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(AreaCodeService::class.java)
        }

//        fun encodedClient(): OkHttpClient {
//            return OkHttpClient.Builder()
//                .addInterceptor(ClientInterceptor())
//                .build()
//        }

//        internal class ClientInterceptor: Interceptor {
//            @Throws(IOException::class)
//            override fun intercept(chain: Interceptor.Chain): Response {
//                val response = chain.proceed(chain.request())
//                return response.newBuilder()
//                    .addHeader("Content-Type", "application/json; charset-utf-8")
//                    .build()
//            }
//        }
    }
}
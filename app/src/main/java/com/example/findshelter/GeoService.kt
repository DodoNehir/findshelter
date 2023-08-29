package com.example.findshelter

import retrofit2.Call
import retrofit2.http.*

interface GeoService {
    //TODO: MAPS_API_KEY local properties에 있는데 어떻게 넣을까..
    @GET("maps/api/geocode/json")
    fun getResults(
        @Query("latlng", encoded = true) lalo: String,
        @Query("key", encoded = true) API_KEY: String,
        @Query("language",encoded = true) language: String
    ): Call<GoogleAddressResponse>

//    companion object {
//        private const val BASE_URL = "https://maps.googleapis.com/"
//
//        fun create(): RetrofitClient {
//            val gson: Gson = GsonBuilder().setLenient().create()
//
//            return Retrofit.Builder()
//                .baseUrl(BASE_URL)
//                .addConverterFactory(GsonConverterFactory.create(gson))
//                .build()
//                .create(RetrofitClient::class.java)
//        }
//    }
}
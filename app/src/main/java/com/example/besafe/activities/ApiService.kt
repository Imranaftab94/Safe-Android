package com.example.besafe.activities

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    @Headers("Content-Type: application/json")
    @POST("send-message")
    fun sendMessage(@Body request: MessageRequest): Call<Void>
}
package ru.netology.nmedia.api

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.dto.AuthResponse

// Отдельный Retrofit для аутентификации (быстрый)
private val authRetrofit = Retrofit.Builder()
    .baseUrl("${BuildConfig.BASE_URL}/")  // обычный URL без /api/slow/
    .addConverterFactory(GsonConverterFactory.create())
    .build()
    interface AuthApiService {
        @FormUrlEncoded
        @POST("api/users/authentication")
        suspend fun authenticate(
            @Field("login") login: String,
            @Field("pass") password: String
        ): Response<AuthResponse>

        // Push уведомления
        @FormUrlEncoded
        @POST("api/pushes")
        suspend fun sendPushToken(
            @Field("token") token: String
        ): Response<Unit>
    }
object AuthApi {
    val service: AuthApiService by lazy {
        authRetrofit.create(AuthApiService::class.java)
    }
}

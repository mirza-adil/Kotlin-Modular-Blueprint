package com.mirza.auth.data.datasource

import api.AuthApi
import kotlinx.coroutines.suspendCancellableCoroutine
import model.LoginRequest
import model.LoginResponse
import model.NetworkResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface AuthRemoteDataSource {

    suspend fun login(email: String, password: String): LoginResponse

    suspend fun logout()
}

class AuthRemoteDataSourceImpl @Inject constructor(
    private val authApi: AuthApi
) : AuthRemoteDataSource {

    override suspend fun login(email: String, password: String): LoginResponse =
        authApi.login(LoginRequest(email, password)).await().unwrap()

    override suspend fun logout() {
        authApi.logout().await().unwrap()
    }
}

private suspend fun <T> Call<T>.await(): T = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val body = response.body()
            if (body != null) {
                continuation.resume(body)
            } else {
                continuation.resumeWithException(IllegalStateException("Empty response body"))
            }
        }

        override fun onFailure(call: Call<T>, throwable: Throwable) {
            continuation.resumeWithException(throwable)
        }
    })
    continuation.invokeOnCancellation { cancel() }
}

private fun <T> NetworkResponse<T>.unwrap(): T = when (this) {
    is NetworkResponse.Success -> data
    is NetworkResponse.ApiError -> throw IllegalStateException(message ?: "API error ($code)")
    is NetworkResponse.NetworkError -> throw error
    is NetworkResponse.UnknownError -> throw error
}
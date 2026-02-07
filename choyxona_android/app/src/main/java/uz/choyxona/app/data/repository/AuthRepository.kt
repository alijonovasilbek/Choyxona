package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.model.TokenResponse
import uz.choyxona.app.data.model.UserLoginRequest
import uz.choyxona.app.data.model.UserRegisterRequest
import uz.choyxona.app.data.model.UserResponse

class AuthRepository {

    private val api = ApiClient.auth

    suspend fun login(username: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.login(
                username = username,
                password = password
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception(response.errorBody()?.string() ?: "Login failed")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(
        fullName: String,
        phone: String,
        username: String,
        password: String
    ): Result<UserResponse> {
        return try {
            val request = UserRegisterRequest(fullName, phone, username, password)
            val response = api.register(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(token: String): Result<UserResponse> {
        return try {
            val response = api.getCurrentUser("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
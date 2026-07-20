package uz.choyxona.app.data.repository

import uz.choyxona.app.data.api.ApiErrorMapper

import uz.choyxona.app.data.api.ApiClient
import uz.choyxona.app.data.model.TokenResponse
import uz.choyxona.app.data.model.FilialInfo
import uz.choyxona.app.data.model.UserLoginRequest
import uz.choyxona.app.data.model.UserRegisterRequest
import uz.choyxona.app.data.model.UserResponse

class AuthRepository {

    private val api = ApiClient.auth

    suspend fun login(username: String, password: String): Result<TokenResponse> {
        return try {
            val response = api.login(UserLoginRequest(username = username, password = password))

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception(ApiErrorMapper.fromResponse(response))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    suspend fun switchFilial(token: String, filialId: Int): Result<TokenResponse> {
        return try {
            val response = api.switchFilial("Bearer $token", filialId)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(
                    Exception(ApiErrorMapper.fromResponse(response))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    suspend fun getAvailableFilials(): Result<List<FilialInfo>> {
        return try {
            val response = api.getAvailableFilials()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.filials)
            } else {
                Result.failure(
                    Exception(ApiErrorMapper.fromResponse(response))
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
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
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }

    suspend fun getCurrentUser(token: String): Result<UserResponse> {
        return try {
            val response = api.getCurrentUser("Bearer $token")

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(ApiErrorMapper.fromResponse(response)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(ApiErrorMapper.fromThrowable(e)))
        }
    }
}

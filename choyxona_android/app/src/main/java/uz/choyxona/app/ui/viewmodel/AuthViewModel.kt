package uz.choyxona.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.choyxona.app.data.local.TokenManager
import uz.choyxona.app.data.model.UserResponse
import uz.choyxona.app.data.repository.AuthRepository

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: UserResponse? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val token = tokenManager.accessToken.first()
                if (!token.isNullOrEmpty()) {
                    // Get current user
                    val result = authRepository.getCurrentUser(token)
                    if (result.isSuccess) {
                        val user = result.getOrNull()
                        if (user != null) {
                            _uiState.value = AuthUiState(
                                isLoggedIn = true,
                                currentUser = user,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.value = AuthUiState(
                            isLoggedIn = false,
                            isLoading = false,
                            error = result.exceptionOrNull()?.message
                        )
                    }
                } else {
                    _uiState.value = AuthUiState(
                        isLoggedIn = false,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = AuthUiState(
                    isLoggedIn = false,
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )

            val result = authRepository.login(username, password)
            if (result.isSuccess) {
                val tokenResponse = result.getOrNull()
                if (tokenResponse != null) {
                    tokenManager.saveTokens(
                        tokenResponse.accessToken,
                        tokenResponse.refreshToken
                    )

                    // Get current user
                    val userResult = authRepository.getCurrentUser(tokenResponse.accessToken)
                    if (userResult.isSuccess) {
                        val user = userResult.getOrNull()
                        if (user != null) {
                            tokenManager.saveUserId(user.id.toString())
                            _uiState.value = AuthUiState(
                                isLoggedIn = true,
                                currentUser = user,
                                isLoading = false
                            )
                        }
                    } else {
                        _uiState.value = AuthUiState(
                            isLoggedIn = true,
                            isLoading = false,
                            error = userResult.exceptionOrNull()?.message
                        )
                    }
                }
            } else {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            _uiState.value = AuthUiState(
                isLoggedIn = false,
                currentUser = null
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
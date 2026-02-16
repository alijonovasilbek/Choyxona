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
import uz.choyxona.app.data.model.UserInfo
import uz.choyxona.app.data.model.FilialInfo
import uz.choyxona.app.data.repository.AuthRepository

data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentUser: UserResponse? = null,
    val userInfo: UserInfo? = null,
    val needsFilialSelection: Boolean = false,
    val availableFilials: List<FilialInfo> = emptyList()
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
                            val needsSelection = isAdminOrSuperAdmin(user.roles)
                            if (needsSelection) {
                                val filialsResult = authRepository.getAvailableFilials()
                                _uiState.value = AuthUiState(
                                    isLoggedIn = false,
                                    currentUser = user,
                                    isLoading = false,
                                    needsFilialSelection = true,
                                    availableFilials = filialsResult.getOrNull() ?: emptyList()
                                )
                            } else {
                                _uiState.value = AuthUiState(
                                    isLoggedIn = true,
                                    currentUser = user,
                                    isLoading = false
                                )
                            }
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

                    val userInfo = tokenResponse.userInfo

                    // Check if user needs to select filial
                    val needsSelection = if (isAdminOrSuperAdmin(userInfo.roles)) {
                        true
                    } else {
                        !userInfo.isOshpaz && userInfo.activeFilialId == null
                    }

                    if (needsSelection) {
                        // Admin/Superadmin with multiple filials - need selection
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            needsFilialSelection = true,
                            userInfo = userInfo,
                            availableFilials = userInfo.availableFilials
                        )
                    } else {
                        // Oshpaz or already has selected filial - proceed
                        // Get current user details
                        val userResult = authRepository.getCurrentUser(tokenResponse.accessToken)
                        if (userResult.isSuccess) {
                            val user = userResult.getOrNull()
                            if (user != null) {
                                tokenManager.saveUserId(user.id.toString())
                                _uiState.value = AuthUiState(
                                    isLoggedIn = true,
                                    currentUser = user,
                                    userInfo = userInfo,
                                    isLoading = false
                                )
                            } else {
                                _uiState.value = AuthUiState(
                                    isLoading = false,
                                    error = "Foydalanuvchi ma'lumoti bo'sh qaytdi"
                                )
                            }
                        } else {
                            _uiState.value = AuthUiState(
                                isLoading = false,
                                error = userResult.exceptionOrNull()?.message ?: "Foydalanuvchi ma'lumotini olishda xatolik"
                            )
                        }
                    }
                } else {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        error = "Login javobi bo'sh qaytdi"
                    )
                }
            } else {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    fun selectFilial(filialId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val currentToken = tokenManager.accessToken.first()
            if (currentToken != null) {
                val result = authRepository.switchFilial(currentToken, filialId)
                if (result.isSuccess) {
                    val tokenResponse = result.getOrNull()
                    if (tokenResponse != null) {
                        // Update tokens
                        tokenManager.saveTokens(
                            tokenResponse.accessToken,
                            tokenResponse.refreshToken
                        )

                        // Get current user
                        val userResult = authRepository.getCurrentUser(tokenResponse.accessToken)
                        if (userResult.isSuccess) {
                            val user = userResult.getOrNull()
                            if (user != null) {
                                _uiState.value = AuthUiState(
                                    isLoggedIn = true,
                                    currentUser = user,
                                    userInfo = tokenResponse.userInfo,
                                    needsFilialSelection = false,
                                    isLoading = false
                                )
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Foydalanuvchi ma'lumoti bo'sh qaytdi"
                                )
                            }
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = userResult.exceptionOrNull()?.message ?: "Foydalanuvchi ma'lumotini olishda xatolik"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = "Filial almashish javobi bo'sh qaytdi"
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Token topilmadi, qayta login qiling"
                )
            }
        }
    }

    fun startFilialSelection() {
        viewModelScope.launch {
            val result = authRepository.getAvailableFilials()
            _uiState.value = _uiState.value.copy(
                needsFilialSelection = true,
                availableFilials = result.getOrNull() ?: _uiState.value.availableFilials,
                error = if (result.isFailure) result.exceptionOrNull()?.message else null
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            _uiState.value = AuthUiState(
                isLoggedIn = false,
                currentUser = null,
                needsFilialSelection = false
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun isAdminOrSuperAdmin(roles: List<String>): Boolean {
        return roles.any {
            it.equals("admin", ignoreCase = true) ||
                it.equals("superadmin", ignoreCase = true) ||
                it.equals("superuser", ignoreCase = true)
        }
    }
}

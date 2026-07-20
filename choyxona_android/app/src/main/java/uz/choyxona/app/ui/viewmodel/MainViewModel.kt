package uz.choyxona.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.choyxona.app.data.local.TokenManager
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.RoomResponse
import uz.choyxona.app.data.model.UserResponse
import uz.choyxona.app.data.repository.BookingRepository
import uz.choyxona.app.data.repository.ReportRepository
import uz.choyxona.app.data.repository.RoomRepository
import uz.choyxona.app.data.repository.UserRepository
import uz.choyxona.app.ui.screen.DashboardStats
import uz.choyxona.app.ui.screen.ReportStats
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookings: List<BookingResponse> = emptyList(),
    val rooms: List<RoomResponse> = emptyList(),
    val users: List<UserResponse> = emptyList(),
    val dashboardStats: DashboardStats? = null,
    val reportStats: ReportStats? = null
)

class MainViewModel(
    private val tokenManager: TokenManager,
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val roomRepository: RoomRepository = RoomRepository(),
    private val reportRepository: ReportRepository = ReportRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private var activeFilialId: Int? = null

    init {
        loadData()
    }

    fun setActiveFilial(filialId: Int?) {
        if (activeFilialId != filialId) {
            activeFilialId = filialId
            loadData()
        }
    }

    fun loadData() {
        loadBookings()
        loadRooms()
        loadUsers()
        loadStats()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = bookingRepository.getAllBookings(
                    token = token,
                    filialId = activeFilialId
                )
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        bookings = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = roomRepository.getAllRooms(
                    token = token,
                    filialId = activeFilialId
                )
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        rooms = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = userRepository.getAllUsers(token)
                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(
                        users = result.getOrNull() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }

    fun loadStats() {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        loadStatsRange(
            dateFrom = today.minusDays(6).format(formatter),
            dateTo = today.format(formatter)
        )
    }

    fun loadStatsRange(dateFrom: String, dateTo: String) {
        viewModelScope.launch {
            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {

                // Load dashboard stats
                val result = reportRepository.getStats(
                    token = token,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                )
                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    if (stats != null) {
                        _uiState.value = _uiState.value.copy(
                            dashboardStats = DashboardStats(
                                totalBookings = stats.totalBookings,
                                pendingCount = stats.kutilmoqdaCount,
                                successfulCount = stats.muvaffaqiyatliCount,
                                cancelledCount = stats.bekorQilindiCount,
                                totalRevenue = stats.totalRevenue
                            ),
                            reportStats = ReportStats(
                                totalBookings = stats.totalBookings,
                                pendingCount = stats.kutilmoqdaCount,
                                successfulCount = stats.muvaffaqiyatliCount,
                                cancelledCount = stats.bekorQilindiCount,
                                totalRevenue = stats.totalRevenue,
                                successfulRevenue = stats.totalRevenue
                            )
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    error = "Not authenticated"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deleteRoom(roomId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val token = tokenManager.accessToken.first()
            if (!token.isNullOrEmpty()) {
                val result = roomRepository.deleteRoom(token, roomId)

                if (result.isSuccess) {
                    // O'chirgandan keyin roomlarni qayta yuklaymiz
                    loadRooms()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
            }
        }
    }
}

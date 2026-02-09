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
import uz.choyxona.app.data.repository.BookingRepository
import uz.choyxona.app.data.repository.ReportRepository
import uz.choyxona.app.data.repository.RoomRepository
import uz.choyxona.app.ui.screen.DashboardStats
import uz.choyxona.app.ui.screen.ReportStats

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookings: List<BookingResponse> = emptyList(),
    val rooms: List<RoomResponse> = emptyList(),
    val dashboardStats: DashboardStats? = null,
    val reportStats: ReportStats? = null
)

class MainViewModel(
    private val tokenManager: TokenManager,
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val roomRepository: RoomRepository = RoomRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var currentToken: String? = null

    init {
        loadToken()
    }

    private fun loadToken() {
        viewModelScope.launch {
            currentToken = tokenManager.accessToken.first()
            if (currentToken != null) {
                loadData()
            }
        }
    }

    fun loadData() {
        loadBookings()
        loadRooms()
        loadStats()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            currentToken?.let { token ->
                val result = bookingRepository.getAllBookings(token)
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
            }
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            currentToken?.let { token ->
                val result = roomRepository.getAllRooms(token)
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
            }
        }
    }

    fun loadStats() {
        viewModelScope.launch {
            currentToken?.let { token ->
                // Load dashboard stats
                val result = reportRepository.getStats(token)
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
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deleteRoom(roomId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            currentToken?.let { token ->
                val result = roomRepository.deleteRoom(token, roomId)

                if (result.isSuccess) {
                    // O‘chirgandan keyin roomlarni qayta yuklaymiz
                    loadRooms()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.exceptionOrNull()?.message
                    )
                }
            }
        }
    }

}
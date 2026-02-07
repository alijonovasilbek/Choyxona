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
import uz.choyxona.app.data.model.BookingStatus
import uz.choyxona.app.data.repository.BookingRepository

data class BookingUiState(
    val bookings: List<BookingResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedBooking: BookingResponse? = null
)

class BookingViewModel(
    private val bookingRepository: BookingRepository = BookingRepository(),
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingUiState())
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    init {
        loadBookings()
    }

    private suspend fun getToken(): String {
        return tokenManager.accessToken.first() ?: ""
    }

    fun loadBookings(
        bookingDate: String? = null,
        roomId: Int? = null,
        statusFilter: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val token = getToken()
            if (token.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
                return@launch
            }

            val result = bookingRepository.getAllBookings(token, bookingDate, roomId, statusFilter)
            if (result.isSuccess) {
                _uiState.value = BookingUiState(
                    bookings = result.getOrNull() ?: emptyList(),
                    isLoading = false
                )
            } else {
                _uiState.value = BookingUiState(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun getBookingById(bookingId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val token = getToken()
            if (token.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
                return@launch
            }

            val result = bookingRepository.getBooking(token, bookingId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    selectedBooking = result.getOrNull(),
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

    fun updateBookingStatus(
        bookingId: Int,
        status: BookingStatus,
        totalAmount: Double? = null,
        cancellationReason: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val token = getToken()
            if (token.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
                return@launch
            }

            val result = bookingRepository.updateBookingStatus(
                token, bookingId, status, totalAmount, cancellationReason
            )
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadBookings()
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun deleteBooking(bookingId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val token = getToken()
            if (token.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Not authenticated"
                )
                return@launch
            }

            val result = bookingRepository.deleteBooking(token, bookingId)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                loadBookings()
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSelectedBooking() {
        _uiState.value = _uiState.value.copy(selectedBooking = null)
    }
}
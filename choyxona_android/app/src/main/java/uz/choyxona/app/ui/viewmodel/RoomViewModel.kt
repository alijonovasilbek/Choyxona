package uz.choyxona.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uz.choyxona.app.data.model.RoomResponse
import uz.choyxona.app.data.repository.RoomRepository

data class RoomUiState(
    val rooms: List<RoomResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedRoom: RoomResponse? = null
)

class RoomViewModel(
    private val roomRepository: RoomRepository,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = _uiState

    init {
        loadRooms()
    }

    fun loadRooms(includeInactive: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            roomRepository.getAllRooms(token, includeInactive).fold(
                onSuccess = { rooms ->
                    _uiState.value = _uiState.value.copy(
                        rooms = rooms,
                        isLoading = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load rooms"
                    )
                }
            )
        }
    }

    fun createRoom(
        name: String,
        description: String?,
        filialId: Int
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            roomRepository.createRoom(token, name, description, filialId).fold(
                onSuccess = {
                    loadRooms()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to create room"
                    )
                }
            )
        }
    }

    fun deleteRoom(roomId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            roomRepository.deleteRoom(token, roomId).fold(
                onSuccess = {
                    loadRooms()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to delete room"
                    )
                }
            )
        }
    }

    fun selectRoom(room: RoomResponse) {
        _uiState.value = _uiState.value.copy(selectedRoom = room)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

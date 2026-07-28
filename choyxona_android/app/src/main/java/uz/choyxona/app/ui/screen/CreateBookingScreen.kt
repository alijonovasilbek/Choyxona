package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.RoomResponse
import uz.choyxona.app.ui.components.GlassButton
import uz.choyxona.app.ui.components.GlassTextField
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*
import uz.choyxona.app.ui.util.roomNameComparator
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBookingScreen(
    rooms: List<RoomResponse>,
    initialRoomId: Int? = null,
    initialDate: String? = null,
    onCreateBooking: (
        roomId: Int,
        date: String,
        description: String?
    ) -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    val parsedInitialDate = remember(initialDate) {
        initialDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    }
    var selectedRoom by remember { mutableStateOf<RoomResponse?>(null) }
    var selectedDate by remember(parsedInitialDate) { mutableStateOf(parsedInitialDate ?: LocalDate.now()) }
    var description by remember { mutableStateOf("") }
    val activeRooms = remember(rooms) {
        rooms.filter { it.isActive }.sortedWith(roomNameComparator { it.name })
    }

    var showRoomPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24L * 60L * 60L * 1000L
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(activeRooms, rooms, initialRoomId) {
        if (selectedRoom == null && initialRoomId != null) {
            selectedRoom = activeRooms.firstOrNull { it.id == initialRoomId }
                ?: rooms.firstOrNull { it.id == initialRoomId }
        }
        if (selectedRoom == null && activeRooms.size == 1) {
            selectedRoom = activeRooms.first()
        }
    }

    // Show error snackbar when error changes
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = ErrorRed,
                        contentColor = Color.White
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(BackgroundLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Orqaga",
                            tint = PrimaryGreen
                        )
                    }

                    Text(
                        text = "Bron yaratish",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.size(48.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    LiquidGlassCard {
                        // Xona tanlash
                        Text(
                            text = "Xona",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showRoomPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (selectedRoom != null) PrimaryGreen else TextSecondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.MeetingRoom,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedRoom?.name ?: "Xonani tanlang",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null
                            )
                        }
                        if (activeRooms.isEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Faol xona topilmadi. Avval xonani faol holatda yarating yoki filialni almashtiring.",
                                fontSize = 12.sp,
                                color = ErrorRed
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Sana
                        Text(
                            text = "Sana",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = PrimaryGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Tavsif
                        GlassTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = "Qo'shimcha ma'lumot (ixtiyoriy)",
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        GlassButton(
                            text = "Bron yaratish",
                            onClick = {
                                selectedRoom?.let { room ->
                                    val formattedDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                    onCreateBooking(
                                        room.id,
                                        formattedDate,
                                        description.ifBlank { null }
                                    )
                                } ?: run {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Xonani tanlang")
                                    }
                                }
                            },
                            enabled = !isLoading,
                            isLoading = isLoading
                        )
                    }
                }
            }

            // Room Picker Dialog
            if (showRoomPicker) {
                AlertDialog(
                    onDismissRequest = { showRoomPicker = false },
                    title = { Text("Xonani tanlang") },
                    text = {
                        if (activeRooms.isEmpty()) {
                            Text(
                                text = "Faol xonalar yo'q",
                                color = TextSecondary
                            )
                        } else {
                            Column {
                                activeRooms.forEach { room ->
                                    OutlinedButton(
                                        onClick = {
                                            selectedRoom = room
                                            showRoomPicker = false
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.MeetingRoom, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(room.name, modifier = Modifier.weight(1f), color = TextPrimary)
                                        Text(room.filialName ?: "", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showRoomPicker = false }) {
                            Text("Yopish")
                        }
                    }
                )
            }

            // Date Picker Dialog
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                            }
                            showDatePicker = false
                        }) {
                            Text("OK")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) {
                            Text("Bekor qilish")
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }
    }
}

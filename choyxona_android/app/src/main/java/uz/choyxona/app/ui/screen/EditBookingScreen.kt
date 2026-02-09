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
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.RoomResponse
import uz.choyxona.app.ui.components.GlassButton
import uz.choyxona.app.ui.components.GlassTextField
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditBookingScreen(
    booking: BookingResponse,
    rooms: List<RoomResponse>,
    onUpdateBooking: (
        roomId: Int?,
        date: String?,
        time: String?,
        customerName: String?,
        customerPhone: String?,
        guestCount: Int?,
        foodDescription: String?,
        description: String?
    ) -> Unit,
    onNavigateBack: () -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var selectedRoom by remember { mutableStateOf(rooms.find { it.id == booking.roomId }) }
    var selectedDate by remember { mutableStateOf(LocalDate.parse(booking.bookingDate)) }
    var selectedTime by remember { mutableStateOf(LocalTime.parse(booking.bookingTime)) }
    var customerName by remember { mutableStateOf(booking.customerName) }
    var customerPhone by remember { mutableStateOf(booking.customerPhone) }
    var guestCount by remember { mutableStateOf(booking.guestCount.toString()) }
    var foodDescription by remember { mutableStateOf(booking.foodDescription) }
    var description by remember { mutableStateOf(booking.description ?: "") }

    var showRoomPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        BackgroundLight,
                        Color.White
                    )
                )
            )
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
                    text = "Bronni tahrirlash",
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

                    Spacer(modifier = Modifier.height(20.dp))

                    // Sana va vaqt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
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
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Vaqt",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showTimePicker = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = PrimaryGreen
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Mijoz ma'lumotlari
                    GlassTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = "Mijoz ismi",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = "Telefon raqami",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = guestCount,
                        onValueChange = { if (it.all { char -> char.isDigit() }) guestCount = it },
                        label = "Odamlar soni",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = foodDescription,
                        onValueChange = { foodDescription = it },
                        label = "Ovqat tavsifi",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Qo'shimcha ma'lumot (ixtiyoriy)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = error,
                            color = ErrorRed,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GlassButton(
                        text = "Yangilash",
                        onClick = {
                            onUpdateBooking(
                                selectedRoom?.id,
                                if (selectedDate.toString() != booking.bookingDate) selectedDate.toString() else null,
                                if (selectedTime.toString() != booking.bookingTime) selectedTime.toString() else null,
                                if (customerName != booking.customerName) customerName else null,
                                if (customerPhone != booking.customerPhone) customerPhone else null,
                                guestCount.toIntOrNull()?.let { if (it != booking.guestCount) it else null },
                                if (foodDescription != booking.foodDescription) foodDescription else null,
                                if (description != booking.description) description.ifBlank { null } else null
                            )
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
                    Column {
                        rooms.filter { it.isActive }.forEach { room ->
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
                                Text(room.name, modifier = Modifier.weight(1f))
                                Text("${room.capacity} kishi", fontSize = 12.sp)
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

        // Time Picker Dialog
        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        selectedTime = LocalTime.of(
                            timePickerState.hour,
                            timePickerState.minute
                        )
                        showTimePicker = false
                    }) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Bekor qilish")
                    }
                },
                text = {
                    TimePicker(state = timePickerState)
                }
            )
        }
    }
}
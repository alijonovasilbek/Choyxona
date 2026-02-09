package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.BookingStatus
import uz.choyxona.app.data.model.getDisplayName
import uz.choyxona.app.ui.components.GlassTextField
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*

@Composable
fun BookingDetailScreen(
    booking: BookingResponse,
    onNavigateBack: () -> Unit,
    onUpdateStatus: (bookingId: Int, status: BookingStatus, totalAmount: Double?, cancellationReason: String?) -> Unit,
    onDeleteBooking: (bookingId: Int) -> Unit,
    isLoading: Boolean = false
) {
    var showStatusDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf(booking.status) }
    var totalAmount by remember { mutableStateOf(booking.totalAmount?.toString() ?: "") }
    var cancellationReason by remember { mutableStateOf(booking.cancellationReason ?: "") }

    val statusColor = when (booking.status) {
        BookingStatus.PENDING -> StatusPending
        BookingStatus.SUCCESSFUL -> StatusSuccessful
        BookingStatus.CANCELLED -> StatusCancelled
    }

    // Status Update Dialog
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Statusni o'zgartirish") },
            text = {
                Column {
                    Text("Yangi statusni tanlang:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Status Options
                    BookingStatus.values().forEach { status ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryGreen
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(status.getDisplayName())
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Conditional Fields
                    when (selectedStatus) {
                        BookingStatus.SUCCESSFUL -> {
                            GlassTextField(
                                value = totalAmount,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                        totalAmount = it
                                    }
                                },
                                label = "Jami summa *",
                                modifier = Modifier.fillMaxWidth(),
                                keyboardType = KeyboardType.Decimal
                            )
                        }
                        BookingStatus.CANCELLED -> {
                            GlassTextField(
                                value = cancellationReason,
                                onValueChange = { cancellationReason = it },
                                label = "Bekor qilish sababi *",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        else -> {}
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = when (selectedStatus) {
                            BookingStatus.SUCCESSFUL -> totalAmount.toDoubleOrNull()
                            else -> null
                        }
                        val reason = when (selectedStatus) {
                            BookingStatus.CANCELLED -> cancellationReason.ifBlank { null }
                            else -> null
                        }

                        onUpdateStatus(booking.id, selectedStatus, amount, reason)
                        showStatusDialog = false
                    }
                ) {
                    Text("Saqlash", color = PrimaryGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Bekor qilish")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Bronni o'chirish") },
            text = { Text("Haqiqatan ham bu bronni o'chirmoqchimisiz?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBooking(booking.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text("Ha, o'chirish", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Yo'q")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundLight, Color.White)
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
                        contentDescription = "Back",
                        tint = PrimaryGreen
                    )
                }

                Text(
                    text = "Bron tafsilotlari",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Status Card
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Holat",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = booking.status.getDisplayName(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        IconButton(
                            onClick = { showStatusDialog = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryGreen.copy(alpha = 0.1f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Change Status",
                                tint = PrimaryGreen
                            )
                        }
                    }
                }

                // Booking Info Card
                LiquidGlassCard {
                    Text(
                        text = "Bron ma'lumotlari",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(label = "Xona", value = booking.roomName)
                    InfoRow(label = "Sana", value = booking.bookingDate.toString())
                    InfoRow(label = "Vaqt", value = booking.bookingTime.toString())
                    InfoRow(label = "Mijoz", value = booking.customerName)
                    InfoRow(label = "Telefon", value = booking.customerPhone)
                    InfoRow(label = "Mehmonlar", value = "${booking.guestCount} kishi")
                    InfoRow(label = "Ovqat", value = booking.foodDescription)

                    if (!booking.description.isNullOrBlank()) {
                        InfoRow(label = "Tavsif", value = booking.description)
                    }
                }

                // Financial Info (if successful)
                if (booking.status == BookingStatus.SUCCESSFUL && booking.totalAmount != null) {
                    LiquidGlassCard {
                        Text(
                            text = "Moliyaviy ma'lumot",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Jami summa",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "${String.format("%.0f", booking.totalAmount)} so'm",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryGreenDark
                            )
                        }
                    }
                }

                // Cancellation Reason (if cancelled)
                if (booking.status == BookingStatus.CANCELLED && !booking.cancellationReason.isNullOrBlank()) {
                    LiquidGlassCard {
                        Text(
                            text = "Bekor qilish sababi",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = booking.cancellationReason,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }
                }

                // System Info
                LiquidGlassCard {
                    Text(
                        text = "Tizim ma'lumotlari",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    InfoRow(label = "Yaratdi", value = booking.createdByName)
                    InfoRow(label = "Yaratilgan", value = booking.createdAt.toString())
                    InfoRow(label = "Yangilangan", value = booking.updatedAt.toString())
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f)
        )
    }
}
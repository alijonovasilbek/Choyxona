package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.BookingResponse
import uz.choyxona.app.data.model.BookingStatus
import uz.choyxona.app.data.model.getDisplayName
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun DayBookingsScreen(
    date: LocalDate,
    bookings: List<BookingResponse>,
    onBack: () -> Unit,
    onBookingClick: (BookingResponse) -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit,
    onChangeStatus: (BookingResponse) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val dayNames = mapOf(
        1 to "Dushanba",
        2 to "Seshanba",
        3 to "Chorshanba",
        4 to "Payshanba",
        5 to "Juma",
        6 to "Shanba",
        7 to "Yakshanba"
    )

    // Xona bo'yicha guruh
    val bookingsByRoom = bookings.groupBy { it.roomName }

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
            // Day header
            LiquidGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryGreen
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = dayNames[date.dayOfWeek.value] ?: "",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreenDark
                        )
                        Text(
                            text = date.format(formatter),
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }

                    Box(modifier = Modifier.size(48.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (bookingsByRoom.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.EventBusy,
                            contentDescription = "No bookings",
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Bu kun uchun bronlar yo'q",
                            fontSize = 16.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Har bir xona uchun
                    bookingsByRoom.forEach { (roomName, roomBookings) ->
                        item {
                            RoomBookingsCard(
                                roomName = roomName,
                                bookings = roomBookings,
                                onBookingClick = onBookingClick,
                                onEditBooking = onEditBooking,
                                onDeleteBooking = onDeleteBooking,
                                onChangeStatus = onChangeStatus
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RoomBookingsCard(
    roomName: String,
    bookings: List<BookingResponse>,
    onBookingClick: (BookingResponse) -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit,
    onChangeStatus: (BookingResponse) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = null
    ) {
        // Room header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

        }

        Spacer(modifier = Modifier.height(12.dp))

        Divider(color = DividerColor)

        Spacer(modifier = Modifier.height(12.dp))

        // Bookings for this room
        bookings.forEach { booking ->
            BookingItemCard(
                booking = booking,
                onBookingClick = onBookingClick,
                onEditBooking = onEditBooking,
                onDeleteBooking = onDeleteBooking,
                onChangeStatus = onChangeStatus
            )

            if (booking != bookings.last()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun BookingItemCard(
    booking: BookingResponse,
    onBookingClick: (BookingResponse) -> Unit,
    onEditBooking: (BookingResponse) -> Unit,
    onDeleteBooking: (BookingResponse) -> Unit,
    onChangeStatus: (BookingResponse) -> Unit
) {
    val currentStatus = booking.status
    val statusColor = when (currentStatus) {
        BookingStatus.KUTILMOQDA -> StatusPending
        BookingStatus.MUVAFFAQIYATLI -> StatusSuccessful
        BookingStatus.BEKOR_QILINDI -> StatusCancelled
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GlassSurface.copy(alpha = 0.3f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = currentStatus.getDisplayName(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = statusColor
                )
            }

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status change button
                // Edit button
                IconButton(
                    onClick = { onEditBooking(booking) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete button
                IconButton(
                    onClick = { onDeleteBooking(booking) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Description
        if (!booking.description.isNullOrBlank()) {
            Text(
                text = booking.description,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 2
            )
        } else {
            Text(
                text = "Tavsif yo'q",
                fontSize = 13.sp,
                color = TextSecondary,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Created by
        Text(
            text = "Yaratdi: ${booking.createdByName}",
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

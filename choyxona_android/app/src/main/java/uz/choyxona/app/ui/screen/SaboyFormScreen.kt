package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.data.model.SaboyResponse
import uz.choyxona.app.ui.components.GlassButton
import uz.choyxona.app.ui.components.GlassCard
import uz.choyxona.app.ui.components.GlassTextField
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.LocalAppColors
import java.time.LocalDate
import java.time.LocalTime

/**
 * Shared create/edit form. Pass [existing] to edit, or null to create.
 * Returns date as ISO "yyyy-MM-dd" and time as "HH:mm:ss" to match the API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaboyFormScreen(
    existing: SaboyResponse? = null,
    filialName: String? = null,
    isLoading: Boolean = false,
    error: String? = null,
    onNavigateBack: () -> Unit,
    onSubmit: (saboyDate: String, saboyTime: String, description: String) -> Unit
) {
    val colors = LocalAppColors.current
    val isEdit = existing != null

    var selectedDate by remember {
        mutableStateOf(
            existing?.saboyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                ?: LocalDate.now()
        )
    }
    var selectedTime by remember {
        mutableStateOf(
            existing?.saboyTime?.let {
                runCatching { LocalTime.parse(it.take(8)) }.getOrNull()
            } ?: LocalTime.of(12, 0)
        )
    }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var descriptionError by remember { mutableStateOf(false) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toEpochDay() * 24L * 60L * 60L * 1000L
    )
    val timePickerState = rememberTimePickerState(
        initialHour = selectedTime.hour,
        initialMinute = selectedTime.minute,
        is24Hour = true
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / (24L * 60L * 60L * 1000L))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Bekor qilish") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Vaqtni tanlang") },
            text = {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Bekor qilish") }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
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
                        tint = colors.primary
                    )
                }

                Text(
                    text = if (isEdit) "Saboyni tahrirlash" else "Saboy qo'shish",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                LiquidGlassCard {
                    if (!filialName.isNullOrBlank()) {
                        Text(
                            text = "Filial: $filialName",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    PickerRow(
                        label = "Sana",
                        value = formatSaboyDate(selectedDate.toString()),
                        onClick = { showDatePicker = true }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    PickerRow(
                        label = "Vaqt",
                        value = String.format("%02d:%02d", selectedTime.hour, selectedTime.minute),
                        isTime = true,
                        onClick = { showTimePicker = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = description,
                        onValueChange = {
                            description = it
                            descriptionError = false
                        },
                        label = "Tavsif *",
                        modifier = Modifier.fillMaxWidth(),
                        isError = descriptionError,
                        errorMessage = if (descriptionError) "Tavsifni kiriting" else ""
                    )

                    if (error != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = error, color = colors.error, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    GlassButton(
                        text = "Saqlash",
                        onClick = {
                            descriptionError = description.isBlank()
                            if (!descriptionError) {
                                onSubmit(
                                    selectedDate.toString(),
                                    String.format(
                                        "%02d:%02d:00",
                                        selectedTime.hour,
                                        selectedTime.minute
                                    ),
                                    description.trim()
                                )
                            }
                        },
                        isLoading = isLoading,
                        enabled = !isLoading
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerRow(
    label: String,
    value: String,
    isTime: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        contentPadding = 14.dp,
        onClick = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isTime) Icons.Default.Schedule else Icons.Default.CalendarMonth,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, fontSize = 12.sp, color = colors.textSecondary)
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
            }
        }
    }
}

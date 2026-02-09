package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.ui.components.GlassButton
import uz.choyxona.app.ui.components.GlassTextField
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*

@Composable
fun CreateRoomScreen(
    onNavigateBack: () -> Unit,
    onCreateRoom: (name: String, description: String?, capacity: Int) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var capacityError by remember { mutableStateOf(false) }

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
                    text = "Xona qo'shish",
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
                    GlassTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = false
                        },
                        label = "Xona nomi *",
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError,
                        errorMessage = if (nameError) "Xona nomini kiriting" else ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Tavsif (ixtiyoriy)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = capacity,
                        onValueChange = {
                            if (it.all { char -> char.isDigit() } || it.isEmpty()) {
                                capacity = it
                                capacityError = false
                            }
                        },
                        label = "Sig'im (odamlar soni) *",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Number,
                        isError = capacityError,
                        errorMessage = if (capacityError) "Sig'imni kiriting" else ""
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
                        text = "Saqlash",
                        onClick = {
                            nameError = name.isBlank()
                            capacityError = capacity.isBlank() || capacity.toIntOrNull() == null || capacity.toInt() <= 0

                            if (!nameError && !capacityError) {
                                onCreateRoom(
                                    name.trim(),
                                    description.trim().ifBlank { null },
                                    capacity.toInt()
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
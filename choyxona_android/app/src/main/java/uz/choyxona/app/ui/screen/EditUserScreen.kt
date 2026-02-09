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
import uz.choyxona.app.data.model.UserResponse
import uz.choyxona.app.ui.components.GlassButton
import uz.choyxona.app.ui.components.GlassTextField
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.*

@Composable
fun EditUserScreen(
    user: UserResponse,
    onNavigateBack: () -> Unit,
    onUpdateUser: (userId: Int, fullName: String, phone: String, telegramChatId: String?, isActive: Boolean, roles: List<String>) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var fullName by remember { mutableStateOf(user.fullName) }
    var phone by remember { mutableStateOf(user.phone) }
    var telegramChatId by remember { mutableStateOf(user.telegramChatId ?: "") }
    var isActive by remember { mutableStateOf(user.isActive) }
    var selectedRoles by remember { mutableStateOf(user.roles.toSet()) }

    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var rolesError by remember { mutableStateOf(false) }

    val availableRoles = listOf(
        "admin" to "Admin",
        "oshpaz" to "Oshpaz"
    )

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
                    text = "Foydalanuvchini tahrirlash",
                    fontSize = 20.sp,
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
                    // Username (readonly)
                    Column {
                        Text(
                            text = "Foydalanuvchi nomi",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = user.username,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            nameError = false
                        },
                        label = "To'liq ism *",
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError,
                        errorMessage = if (nameError) "Ismni kiriting" else ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            phoneError = false
                        },
                        label = "Telefon raqami *",
                        modifier = Modifier.fillMaxWidth(),
                        keyboardType = KeyboardType.Phone,
                        isError = phoneError,
                        errorMessage = if (phoneError) "Telefon raqamini kiriting" else ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = telegramChatId,
                        onValueChange = { telegramChatId = it },
                        label = "Telegram Chat ID (ixtiyoriy)",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Active/Inactive Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Foydalanuvchi holati",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isActive) "Faol" else "Nofaol",
                                fontSize = 14.sp,
                                color = if (isActive) PrimaryGreen else Color.Gray,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = isActive,
                                onCheckedChange = { isActive = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = PrimaryGreen,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = Color.Gray
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column {
                        Text(
                            text = "Rollar *",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        availableRoles.forEach { (roleValue, roleDisplay) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selectedRoles.contains(roleValue),
                                    onCheckedChange = { checked ->
                                        selectedRoles = if (checked) {
                                            selectedRoles + roleValue
                                        } else {
                                            selectedRoles - roleValue
                                        }
                                        rolesError = false
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryGreen
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = roleDisplay,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                            }
                        }

                        if (rolesError) {
                            Text(
                                text = "Kamida bitta rolni tanlang",
                                color = ErrorRed,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                            )
                        }
                    }

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
                            nameError = fullName.isBlank()
                            phoneError = phone.isBlank()
                            rolesError = selectedRoles.isEmpty()

                            if (!nameError && !phoneError && !rolesError) {
                                onUpdateUser(
                                    user.id,
                                    fullName.trim(),
                                    phone.trim(),
                                    telegramChatId.trim().ifBlank { null },
                                    isActive,
                                    selectedRoles.toList()
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
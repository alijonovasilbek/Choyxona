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
fun CreateUserScreen(
    onNavigateBack: () -> Unit,
    onCreateUser: (fullName: String, phone: String, username: String, password: String, telegramChatId: String?, roles: List<String>) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var telegramChatId by remember { mutableStateOf("") }
    var selectedRoles by remember { mutableStateOf(setOf<String>()) }

    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var rolesError by remember { mutableStateOf(false) }

    val availableRoles = listOf(
        "admin" to "Admin",
        "oshpaz" to "Oshpaz"
    )

    // Telefon raqamini formatlash funksiyasi
    val formatPhoneNumber: (String) -> String = { input ->
        when {
            input.startsWith("+998") -> input
            input.startsWith("998") -> "+$input"
            input.length == 9 -> "+998$input"
            else -> input
        }
    }

    // Telefon raqamini tekshirish
    val isPhoneValid: (String) -> Boolean = { phoneNum ->
        val formatted = formatPhoneNumber(phoneNum)
        formatted.matches(Regex("^\\+998\\d{9}$"))
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
                    text = "Foydalanuvchi qo'shish",
                    fontSize = 22.sp,
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

                    // Telefon raqami - TO'G'RILANDI
                    Column {
                        GlassTextField(
                            value = phone,
                            onValueChange = {
                                // Faqat raqam va + belgisini qabul qilish
                                if (it.isEmpty() || it.matches(Regex("^[+]?[0-9]*$"))) {
                                    phone = it
                                    phoneError = false
                                }
                            },
                            label = "Telefon raqami *",
                            modifier = Modifier.fillMaxWidth(),
                            keyboardType = KeyboardType.Phone,
                            isError = phoneError,
                            errorMessage = if (phoneError) "Noto'g'ri telefon formati" else ""
                        )

                        // Helper text
                        Text(
                            text = "Masalan: +998901234567 yoki 998901234567",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            usernameError = false
                        },
                        label = "Foydalanuvchi nomi *",
                        modifier = Modifier.fillMaxWidth(),
                        isError = usernameError,
                        errorMessage = if (usernameError) "Foydalanuvchi nomini kiriting" else ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            passwordError = false
                        },
                        label = "Parol *",
                        modifier = Modifier.fillMaxWidth(),
                        isPassword = true,
                        isError = passwordError,
                        errorMessage = if (passwordError) "Parolni kiriting (min 6 belgi)" else ""
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    GlassTextField(
                        value = telegramChatId,
                        onValueChange = { telegramChatId = it },
                        label = "Telegram Chat ID (ixtiyoriy)",
                        modifier = Modifier.fillMaxWidth()
                    )

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
                        text = "Saqlash",
                        onClick = {
                            nameError = fullName.isBlank()
                            usernameError = username.isBlank()
                            passwordError = password.length < 6
                            rolesError = selectedRoles.isEmpty()

                            // Telefon validatsiyasi - TO'G'RILANDI
                            val formattedPhone = formatPhoneNumber(phone)
                            phoneError = !formattedPhone.matches(Regex("^\\+998\\d{9}$"))

                            if (!nameError && !phoneError && !usernameError && !passwordError && !rolesError) {
                                onCreateUser(
                                    fullName.trim(),
                                    formattedPhone, // Formatlanigan telefon yuboriladi
                                    username.trim(),
                                    password,
                                    telegramChatId.trim().ifBlank { null },
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
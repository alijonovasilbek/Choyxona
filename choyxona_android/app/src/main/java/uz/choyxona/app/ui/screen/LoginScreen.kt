package uz.choyxona.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uz.choyxona.app.ui.components.GlassButton
import uz.choyxona.app.ui.components.GlassTextField
import uz.choyxona.app.ui.components.LiquidGlassCard
import uz.choyxona.app.ui.theme.BackgroundLight
import uz.choyxona.app.ui.theme.PrimaryGreen
import uz.choyxona.app.ui.theme.PrimaryGreenDark
import uz.choyxona.app.ui.theme.TextPrimary
import uz.choyxona.app.ui.theme.TextSecondary
import uz.choyxona.app.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isLoggedIn, uiState.needsFilialSelection) {
        if (uiState.isLoggedIn || uiState.needsFilialSelection) {
            onLoginSuccess()
        }
    }

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
        // Decorative circles
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = (-150).dp)
                .size(300.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryGreen.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
        )

        Box(
            modifier = Modifier
                .offset(x = (200).dp, y = (400).dp)
                .size(250.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            PrimaryGreenDark.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choyxona",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Bron Tizimi",
                fontSize = 16.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(48.dp))

            LiquidGlassCard {
                GlassTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        viewModel.clearError()
                    },
                    label = "Foydalanuvchi nomi",
                    modifier = Modifier.fillMaxWidth(),
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                )

                Spacer(modifier = Modifier.height(20.dp))

                GlassTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        viewModel.clearError()
                    },
                    label = "Parol",
                    modifier = Modifier.fillMaxWidth(),
                    isPassword = true,
                    imeAction = ImeAction.Done,
                    onImeAction = {
                        if (username.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(username, password)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                GlassButton(
                    text = "Kirish",
                    onClick = {
                        viewModel.login(username, password)
                    },
                    enabled = username.isNotBlank() && password.isNotBlank(),
                    isLoading = uiState.isLoading
                )

                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = uiState.error!!,
                        color = Color.Red,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

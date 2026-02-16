package uz.choyxona.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.choyxona.app.ui.theme.GlassSurface
import uz.choyxona.app.ui.theme.GlassWhite

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    backgroundColor: Color = GlassWhite,
    contentPadding: Dp = 16.dp,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clickable { onClick() }
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.5f)
                    )
                ),
                RoundedCornerShape(cornerRadius)
            )
            .padding(contentPadding)
    } else {
        modifier
            .shadow(elevation, RoundedCornerShape(cornerRadius))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = 0.5f)
                    )
                ),
                RoundedCornerShape(cornerRadius)
            )
            .padding(contentPadding)
    }

    Column(
        modifier = cardModifier,
        content = content
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                spotColor = Color.White.copy(alpha = 0.3f),
                ambientColor = Color.White.copy(alpha = 0.2f)
            )
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        GlassWhite,
                        GlassSurface,
                        GlassWhite.copy(alpha = 0.8f)
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

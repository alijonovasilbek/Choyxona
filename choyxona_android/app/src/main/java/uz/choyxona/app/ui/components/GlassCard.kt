package uz.choyxona.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uz.choyxona.app.ui.theme.LocalAppColors

/**
 * Flat modern card: solid surface, 1px border, soft shadow.
 * (Name kept for compatibility with existing screens.)
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    backgroundColor: Color = Color.Unspecified,
    contentPadding: Dp = 16.dp,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val bg = if (backgroundColor == Color.Unspecified) colors.surface else backgroundColor
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }

    val base = modifier
        .shadow(
            elevation = if (colors.isDark) 0.dp else 2.dp,
            shape = shape,
            spotColor = Color(0x1A0F1728),
            ambientColor = Color(0x0D0F1728)
        )
        .clip(shape)
        .background(bg)
        .border(1.dp, colors.border, shape)

    val cardModifier = if (onClick != null) {
        base.clickable(
            interactionSource = interactionSource,
            indication = rememberRipple(color = colors.primary)
        ) { onClick() }
            .padding(contentPadding)
    } else {
        base.padding(contentPadding)
    }

    Column(
        modifier = cardModifier,
        content = content
    )
}

/**
 * Prominent card for forms / hero sections. Same flat design language,
 * slightly larger radius and padding.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (colors.isDark) 0.dp else 6.dp,
                shape = shape,
                spotColor = Color(0x140F1728),
                ambientColor = Color(0x0A0F1728)
            )
            .clip(shape)
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, shape)
            .padding(20.dp),
        content = content
    )
}

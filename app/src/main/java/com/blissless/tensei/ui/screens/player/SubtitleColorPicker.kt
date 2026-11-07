package com.blissless.tensei.ui.screens.player

import android.app.Activity
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.blissless.tensei.R
import com.blissless.tensei.data.models.SubtitleSettings
import com.blissless.tensei.ui.theme.Spacing
import kotlin.math.roundToInt

// Immediate color picker content for subtitle colors, plus HSL helpers.



// ===========================================================================
// Color Picker
// ===========================================================================
@Composable
internal fun ImmediateColorPickerContent(
    initialColor: Color,
    onColorChange: (Color) -> Unit,
) {
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var lightness by remember { mutableFloatStateOf(0.5f) }
    var alpha by remember { mutableFloatStateOf(initialColor.alpha) }

    LaunchedEffect(Unit) {
        val hsl = rgbToHsl(initialColor)
        hue = hsl[0]; saturation = hsl[1]; lightness = hsl[2]; alpha = hsl[3]
    }

    val currentColor = remember(hue, saturation, lightness, alpha) {
        Color.hsl(hue, saturation, lightness, alpha)
    }

    Column(
        modifier = Modifier.padding(horizontal = Spacing.gutter).padding(bottom = Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val paletteSize = 220.dp
        Box(
            modifier = Modifier
                .size(paletteSize)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val xFraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val yFraction = (offset.y / size.height).coerceIn(0f, 1f)
                        val newHue = xFraction * 360f
                        val newLight = 1f - yFraction
                        hue = newHue; saturation = 1f; lightness = newLight
                        onColorChange(Color.hsl(newHue, 1f, newLight, alpha))
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (x in 0 until size.width.toInt() step 2) {
                    val currentHue = x / size.width * 360f
                    drawRect(
                        color = Color.hsl(currentHue, 1f, 0.5f),
                        topLeft = Offset(x.toFloat(), 0f),
                        size = Size(2f, size.height)
                    )
                }
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0f), Color.Black),
                        startY = 0f,
                        endY = size.height
                    )
                )
                val indicatorX = hue / 360f * size.width
                val indicatorY = (1f - lightness) * size.height
                drawCircle(Color.White, radius = 6f, center = Offset(indicatorX, indicatorY), style = Stroke(width = 2f))
                drawCircle(Color.Black, radius = 4f, center = Offset(indicatorX, indicatorY))
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(currentColor)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(12.dp))
            Slider(
                value = alpha,
                onValueChange = { alpha = it; onColorChange(Color.hsl(hue, saturation, lightness, it)) },
                valueRange = 0f..1f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    inactiveTrackColor = Color.White.copy(alpha = 0.08f)
                )
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "${(alpha * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(36.dp),
                textAlign = TextAlign.End
            )
        }

        Spacer(Modifier.height(8.dp))

        var hex by remember {
            mutableStateOf(
                String.format(
                    "%02X%02X%02X",
                    (currentColor.red * 255).toInt(),
                    (currentColor.green * 255).toInt(),
                    (currentColor.blue * 255).toInt()
                )
            )
        }
        OutlinedTextField(
            value = hex,
            onValueChange = {
                val filtered = it.take(6).uppercase().filter { c -> c in "0123456789ABCDEF" }
                hex = filtered
                if (filtered.length == 6) {
                    val r = filtered.substring(0, 2).toInt(16) / 255f
                    val g = filtered.substring(2, 4).toInt(16) / 255f
                    val b = filtered.substring(4, 6).toInt(16) / 255f
                    val newColor = Color(r, g, b, alpha)
                    val hsl = rgbToHsl(newColor)
                    hue = hsl[0]; saturation = hsl[1]; lightness = hsl[2]
                    onColorChange(newColor)
                }
            },
            label = { Text(stringResource(R.string.subtitle_hex), color = Color.White.copy(alpha = 0.5f)) },
            placeholder = { Text("RRGGBB", color = Color.White.copy(alpha = 0.2f)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.4f)
            )
        )
    }
}


// ===========================================================================
// Utility: RGB → HSL
// ===========================================================================
private fun rgbToHsl(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val lightness = (max + min) / 2f
    if (delta == 0f) return floatArrayOf(0f, 0f, lightness, color.alpha)
    val saturation = if (lightness <= 0.5f) delta / (max + min) else delta / (2f - max - min)
    val hue = when (max) {
        r -> ((g - b) / delta + if (g < b) 6f else 0f)
        g -> ((b - r) / delta + 2f)
        else -> ((r - g) / delta + 4f)
    } * 60f
    return floatArrayOf(hue.coerceAtLeast(0f), saturation, lightness, color.alpha)
}


internal data class RectF(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun width() = right - left
    fun height() = bottom - top
}

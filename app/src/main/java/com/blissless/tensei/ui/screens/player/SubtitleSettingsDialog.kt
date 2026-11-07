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

internal data class TemplateBg(val name: String, val colors: List<Color>)


internal val TEMPLATES = listOf(
    TemplateBg("Dark Scene", listOf(Color(0xFF1a1a2e), Color(0xFF16213e))),
    TemplateBg("Daylight", listOf(Color(0xFF87CEEB), Color(0xFF98D8C8))),
    TemplateBg("Sunset", listOf(Color(0xFFFF6B35), Color(0xFFFFD93D))),
    TemplateBg("Forest", listOf(Color(0xFF2D5016), Color(0xFF4A7C2E))),
    TemplateBg("Night Sky", listOf(Color(0xFF0a0a2e), Color(0xFF1a1a4e))),
    TemplateBg("Beach", listOf(Color(0xFF0077B6), Color(0xFFF4A261))),
    TemplateBg("Solid Black", listOf(Color.Black)),
    TemplateBg("Solid White", listOf(Color.White)),
)


internal val OUTLINE_PRESETS = listOf(
    Color.Black, Color.White, Color.Red, Color(0xFFFF8C00),
    Color.Yellow, Color.Green, Color.Blue, Color.Magenta
)


internal val SHADOW_PRESETS = listOf(
    Color.Black, Color.White, Color(0xFF333333), Color(0xFF555555), Color(0xFF777777)
)


internal val BG_SUB_PRESETS = listOf(
    0x00000000L, 0x40000000L, 0x80000000L, 0xC0000000L,
    0x40FFFFFFL, 0x80FFFFFFL, 0xFF000000L, 0xFFFFFFFFL
)


data class SubtitleFullSettings(
    val fontSize: Float = 22f,
    val fontColor: Long = 0xFFFFFFFFL,
    val enableOutline: Boolean = true,
    val outlineWidth: Float = 2f,
    val outlineColor: Long = 0xFF000000L,
    val enableShadow: Boolean = false,
    val shadowBlur: Float = 2f,
    val shadowOffsetX: Float = 2f,
    val shadowOffsetY: Float = 2f,
    val shadowColor: Long = 0xFF000000L,
    val backgroundColor: Long = 0x00000000L,
    val verticalPosition: Float = 0.9f,
    val horizontalPosition: Float = 0.5f,
    val maxWidthRatio: Float = 0.95f,
    val delayMs: Int = 0,
    val fontFamily: String = "Default",
    val profileName: String = "Default"
) {
    fun toLegacy(): SubtitleSettings = SubtitleSettings(
        fontSize = fontSize,
        fontColor = fontColor,
        enableShadow = enableShadow,
        enableOutline = enableOutline,
        outlineWidth = outlineWidth,
        outlineColor = outlineColor,
        shadowBlur = shadowBlur,
        shadowOffsetX = shadowOffsetX,
        shadowOffsetY = shadowOffsetY,
        shadowColor = shadowColor,
        fontFamily = fontFamily,
        backgroundColor = backgroundColor,
        verticalPosition = verticalPosition,
        horizontalPosition = horizontalPosition,
        maxWidthRatio = maxWidthRatio,
        delayMs = delayMs,
        profileName = profileName
    )
}


internal object Defaults {
    val FULL_SETTINGS = SubtitleFullSettings()
}


// Dark surface for video overlay panels (always dark regardless of theme)
internal val PanelSurface = Color(0xFF121220)

// PanelDivider moved to SubtitleSettingsComponents.kt (internal)

internal val panelShape = RoundedCornerShape(20.dp, 20.dp, 0.dp, 0.dp)

internal val cardShape = RoundedCornerShape(14.dp)

internal val chipShape = RoundedCornerShape(10.dp)


// ---------------------------------------------------------------------------
// Entry point
// ---------------------------------------------------------------------------
@RequiresApi(Build.VERSION_CODES.R)
@Composable
fun SubtitleSettingsDialog(
    currentSettings: SubtitleSettings,
    profiles: List<SubtitleSettings>,
    activeProfileIndex: Int,
    onSettingsChange: (SubtitleSettings) -> Unit,
    onProfileSelect: (Int) -> Unit,
    onResetProfile: (Int) -> Unit,
    onRenameProfile: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {
        SubtitleSettingsContent(
            currentSettings = currentSettings,
            profiles = profiles,
            activeProfileIndex = activeProfileIndex,
            onSettingsChange = onSettingsChange,
            onProfileSelect = onProfileSelect,
            onResetProfile = onResetProfile,
            onRenameProfile = onRenameProfile,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

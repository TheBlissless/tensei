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

// Overlays inside the subtitle settings dialog: rename confirmation,
// rename input and the live subtitle preview.



// Reusable UI components moved to SubtitleSettingsComponents.kt
// (PanelHeader, ResetTextButton, SectionDivider, ToggleRow, SliderRow,
//  ToolbarIconButton, BottomToolbarButton, ColorSwatchRow, ThemeColorItem)

// Panels moved to SubtitleSettingsPanels.kt
// (TemplatePanel, TextSizePanel, OutlinePanel, ShadowPanel, BgPanel,
//  FontColorPanel, FontFamilyPanel)

// ===========================================================================
// CONFIRMATION / RENAME OVERLAYS
// ===========================================================================
@Composable
internal fun ConfirmationOverlay(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.subtitle_confirm),
    onConfirm: () -> Unit,
    dismissLabel: String? = stringResource(R.string.subtitle_cancel),
    onDismiss: () -> Unit,
    cancelLabel: String? = null,
    onCancel: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = PanelSurface,
            shape = cardShape,
            shadowElevation = 32.dp,
            modifier = Modifier.padding(32.dp).width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (dismissLabel != null) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text(dismissLabel, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    if (cancelLabel != null && onCancel != null) {
                        TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                            Text(cancelLabel, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = chipShape
                    ) {
                        Text(confirmLabel, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


@Composable
internal fun RenameOverlay(
    currentName: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = PanelSurface,
            shape = cardShape,
            shadowElevation = 32.dp,
            modifier = Modifier.padding(32.dp).width(320.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.subtitle_rename_profile_title),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = currentName,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.subtitle_profile_name), color = Color.White.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color.White),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.subtitle_cancel), color = Color.White.copy(alpha = 0.6f))
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = chipShape
                    ) {
                        Text(stringResource(R.string.subtitle_rename), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}


// ===========================================================================
// Subtitle Preview
// ===========================================================================
@Composable
internal fun SubtitlePreview(
    settings: SubtitleFullSettings,
    offsetX: Float,
    offsetY: Float,
    onDrag: (Float, Float, Int, Int) -> Unit,
    onDragEnd: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val previewSample = stringResource(R.string.subtitle_preview_sample)
    val textColor = if (settings.fontColor == 0x00000000L) Color.White else Color(settings.fontColor)
    val bgColor = Color(settings.backgroundColor)
    val outlineColor = Color(settings.outlineColor)

    val fontFamily = when (settings.fontFamily) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Sans Serif Light" -> FontFamily.SansSerif
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
    val fontWeight = if (settings.fontFamily == "Sans Serif Light") FontWeight.Light else FontWeight.Normal

    var boxWidth by remember { mutableIntStateOf(0) }
    var boxHeight by remember { mutableIntStateOf(0) }
    val textMeasurer = rememberTextMeasurer()
    val defaultStyle = LocalTextStyle.current
    val shadowColor = Color(settings.shadowColor).copy(alpha = 0.35f)
    val shadowOffsetPx = with(LocalDensity.current) { Offset(settings.shadowOffsetX.dp.toPx(), settings.shadowOffsetY.dp.toPx()) }
    val outlineWidthPx = with(LocalDensity.current) { settings.outlineWidth.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (offsetX - boxWidth / 2f).roundToInt(),
                        (offsetY - boxHeight / 2f).roundToInt()
                    )
                }
                .onSizeChanged { boxWidth = it.width; boxHeight = it.height }
        ) {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragEnd,
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y, boxWidth, boxHeight)
                            }
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (settings.backgroundColor == 0x00000000L) Color.Transparent else bgColor,
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = previewSample,
                            color = textColor,
                            fontSize = settings.fontSize.sp,
                            fontFamily = fontFamily,
                            fontWeight = fontWeight,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.drawWithContent {
                                val textWidth = size.width.toInt()
                                val base = defaultStyle.merge(
                                    TextStyle(
                                        fontSize = settings.fontSize.sp,
                                        fontFamily = fontFamily,
                                        fontWeight = fontWeight,
                                        textAlign = TextAlign.Center
                                    )
                                )
                                if (settings.enableShadow) {
                                    val shadowLayout = textMeasurer.measure(
                                        text = AnnotatedString(previewSample),
                                        style = base.copy(color = shadowColor),
                                        constraints = Constraints(maxWidth = textWidth),
                                        maxLines = 2
                                    )
                                    drawText(shadowLayout, topLeft = shadowOffsetPx)
                                }
                                if (settings.enableOutline) {
                                    val outlineLayout = textMeasurer.measure(
                                        text = AnnotatedString(previewSample),
                                        style = base.copy(
                                            color = outlineColor,
                                            drawStyle = Stroke(width = outlineWidthPx)
                                        ),
                                        constraints = Constraints(maxWidth = textWidth),
                                        maxLines = 2
                                    )
                                    drawText(outlineLayout)
                                }
                                drawContent()
                            }
                        )
                    }
                }
            }
        }
    }
}

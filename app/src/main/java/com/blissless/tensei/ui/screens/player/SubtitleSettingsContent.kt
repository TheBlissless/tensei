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

// The main body of the subtitle settings dialog (template picker, preview
// and panel container).



@RequiresApi(Build.VERSION_CODES.R)
@Composable
internal fun SubtitleSettingsContent(
    currentSettings: SubtitleSettings,
    profiles: List<SubtitleSettings>,
    activeProfileIndex: Int,
    onSettingsChange: (SubtitleSettings) -> Unit,
    onProfileSelect: (Int) -> Unit,
    onResetProfile: (Int) -> Unit,
    onRenameProfile: (Int, String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    var fullSettings by remember { mutableStateOf(
        SubtitleFullSettings(
            fontSize = currentSettings.fontSize,
            fontColor = currentSettings.fontColor,
            enableOutline = currentSettings.enableOutline,
            outlineWidth = currentSettings.outlineWidth,
            outlineColor = currentSettings.outlineColor,
            enableShadow = currentSettings.enableShadow,
            shadowBlur = currentSettings.shadowBlur,
            shadowOffsetX = currentSettings.shadowOffsetX,
            shadowOffsetY = currentSettings.shadowOffsetY,
            shadowColor = currentSettings.shadowColor,
            backgroundColor = currentSettings.backgroundColor,
            verticalPosition = currentSettings.verticalPosition,
            horizontalPosition = currentSettings.horizontalPosition,
            maxWidthRatio = currentSettings.maxWidthRatio,
            delayMs = currentSettings.delayMs,
            fontFamily = currentSettings.fontFamily,
            profileName = currentSettings.profileName
        )
    ) }

    var selectedTemplateIndex by remember { mutableIntStateOf(0) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var showCloseConfirm by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var hasChanges by remember { mutableStateOf(false) }
    var showSaved by remember { mutableStateOf(false) }
    var activePanel by remember { mutableStateOf<Panel?>(null) }

    LaunchedEffect(showSaved) { if (showSaved) { kotlinx.coroutines.delay(2000); showSaved = false } }

    // Sync fullSettings when the external profile changes
    LaunchedEffect(activeProfileIndex) {
        val p = currentSettings
        fullSettings = SubtitleFullSettings(
            fontSize = p.fontSize,
            fontColor = p.fontColor,
            enableOutline = p.enableOutline,
            outlineWidth = p.outlineWidth,
            outlineColor = p.outlineColor,
            enableShadow = p.enableShadow,
            shadowBlur = p.shadowBlur,
            shadowOffsetX = p.shadowOffsetX,
            shadowOffsetY = p.shadowOffsetY,
            shadowColor = p.shadowColor,
            backgroundColor = p.backgroundColor,
            verticalPosition = p.verticalPosition,
            horizontalPosition = p.horizontalPosition,
            maxWidthRatio = p.maxWidthRatio,
            delayMs = p.delayMs,
            fontFamily = p.fontFamily,
            profileName = p.profileName
        )
        dragOffsetX = 0f
        dragOffsetY = 0f
        hasChanges = false
    }

    // Helper: mark state as user-changed
    fun uiChange(block: () -> Unit) {
        block()
        hasChanges = true
    }

    var actualWidth by remember { mutableIntStateOf(1080) }
    var actualHeight by remember { mutableIntStateOf(1920) }

    LaunchedEffect(fullSettings) {
        onSettingsChange(fullSettings.toLegacy())
    }

    fun commitDrag() {
        if (dragOffsetX != 0f || dragOffsetY != 0f) {
            val newV = ((fullSettings.verticalPosition * actualHeight + dragOffsetY) / actualHeight).coerceIn(0.05f, 0.95f)
            val newH = ((fullSettings.horizontalPosition * actualWidth + dragOffsetX) / actualWidth).coerceIn(0.05f, 0.95f)
            fullSettings = fullSettings.copy(verticalPosition = newV, horizontalPosition = newH)
            dragOffsetX = 0f
            dragOffsetY = 0f
            hasChanges = true
        }
    }

    val view = LocalView.current
    val window = (view.context as Activity).window
    LaunchedEffect(Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    val template = TEMPLATES[selectedTemplateIndex]
    val gradient = if (template.colors.size == 1) {
        Brush.verticalGradient(listOf(template.colors[0], template.colors[0]))
    } else {
        Brush.verticalGradient(template.colors)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                actualWidth = size.width
                actualHeight = size.height
            }
    ) {
        // Background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cw = size.width
            val ch = size.height
            val aspect = 16f / 9f
            val dest = run {
                val curr = cw / ch
                if (curr > aspect) {
                    val w = ch * aspect
                    RectF((cw - w) / 2, 0f, (cw + w) / 2, ch)
                } else {
                    val h = cw / aspect
                    RectF(0f, (ch - h) / 2, cw, (ch + h) / 2)
                }
            }
            drawRect(
                brush = gradient,
                topLeft = Offset(dest.left, dest.top),
                size = Size(dest.width(), dest.height())
            )
        }

        val destRect = remember(actualWidth, actualHeight) {
            val cw = actualWidth.toFloat()
            val ch = actualHeight.toFloat()
            val aspect = 16f / 9f
            val curr = cw / ch
            if (curr > aspect) {
                val w = ch * aspect
                RectF((cw - w) / 2, 0f, (cw + w) / 2, ch)
            } else {
                val h = cw / aspect
                RectF(0f, (ch - h) / 2, cw, (ch + h) / 2)
            }
        }

        // ── Top bar ─────────────────────────────────────────────────────────
        var showProfileDropdown by remember { mutableStateOf(false) }
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { showProfileDropdown = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = profiles.getOrNull(activeProfileIndex)?.profileName ?: stringResource(R.string.subtitle_profile),
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "▾",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = showProfileDropdown,
                        onDismissRequest = { showProfileDropdown = false },
                        shape = cardShape
                    ) {
                        profiles.forEachIndexed { idx, p ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        p.profileName,
                                        fontWeight = if (idx == activeProfileIndex) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    commitDrag()
                                    onProfileSelect(idx)
                                    showProfileDropdown = false
                                }
                            )
                        }
                    }
                }

                ToolbarIconButton(Icons.Default.Edit, stringResource(R.string.subtitle_rename)) {
                    renameText = profiles.getOrNull(activeProfileIndex)?.profileName ?: ""
                    showRenameDialog = true
                }
                ToolbarIconButton(if (showSaved) Icons.Default.Check else Icons.Default.Save, stringResource(R.string.subtitle_save)) {
                    if (!showSaved) { commitDrag(); onSave(); hasChanges = false; showSaved = true }
                }
                ToolbarIconButton(Icons.Default.Close, stringResource(R.string.subtitle_close)) {
                    if (hasChanges) showCloseConfirm = true
                    else onDismiss()
                }
                ToolbarIconButton(Icons.Default.RestartAlt, stringResource(R.string.subtitle_reset)) {
                    showResetConfirm = true
                }

                Spacer(Modifier.weight(1f))

                BottomToolbarButton(Icons.Default.Palette, stringResource(R.string.subtitle_template), activePanel == Panel.Template) {
                    activePanel = if (activePanel == Panel.Template) null else Panel.Template
                }
                BottomToolbarButton(Icons.Default.FormatSize, stringResource(R.string.subtitle_size), activePanel == Panel.TextSize) {
                    activePanel = if (activePanel == Panel.TextSize) null else Panel.TextSize
                }
                BottomToolbarButton(Icons.Default.BorderColor, stringResource(R.string.subtitle_outline), activePanel == Panel.Outline) {
                    activePanel = if (activePanel == Panel.Outline) null else Panel.Outline
                }
                BottomToolbarButton(Icons.Default.BlurOn, stringResource(R.string.subtitle_shadow), activePanel == Panel.Shadow) {
                    activePanel = if (activePanel == Panel.Shadow) null else Panel.Shadow
                }
                BottomToolbarButton(Icons.Default.FormatColorFill, stringResource(R.string.subtitle_bg), activePanel == Panel.Bg) {
                    activePanel = if (activePanel == Panel.Bg) null else Panel.Bg
                }
                BottomToolbarButton(Icons.Default.FormatColorText, stringResource(R.string.subtitle_color), activePanel == Panel.FontColor) {
                    activePanel = if (activePanel == Panel.FontColor) null else Panel.FontColor
                }
                BottomToolbarButton(Icons.Default.Tune, stringResource(R.string.subtitle_font), activePanel == Panel.FontFamily) {
                    activePanel = if (activePanel == Panel.FontFamily) null else Panel.FontFamily
                }
            }
        }

        // ── Subtitle preview (rendered AFTER UI so it's on top visually) ────
        val baseX = fullSettings.horizontalPosition * actualWidth
        val baseY = fullSettings.verticalPosition * actualHeight
        SubtitlePreview(
            settings = fullSettings,
            offsetX = baseX + dragOffsetX,
            offsetY = baseY + dragOffsetY,
            onDrag = { dx, dy, bw, bh ->
                val bx = fullSettings.horizontalPosition * actualWidth
                val by = fullSettings.verticalPosition * actualHeight
                val newOx = (bx + dragOffsetX + dx).coerceIn(destRect.left + bw / 2f, destRect.right - bw / 2f)
                val newOy = (by + dragOffsetY + dy).coerceIn(destRect.top + bh / 2f, destRect.bottom - bh / 2f)
                dragOffsetX = newOx - bx
                dragOffsetY = newOy - by
            },
            onDragEnd = ::commitDrag,
            modifier = Modifier.fillMaxSize()
        )

        // ── Bottom panels (slide up) ────────────────────────────────────────
        activePanel?.let { panel ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        color = PanelSurface,
                        shape = panelShape,
                        shadowElevation = 24.dp
                    ) {
                        when (panel) {
                            Panel.Template -> TemplatePanel(
                                selectedIndex = selectedTemplateIndex,
                                onSelect = { idx ->
                                    selectedTemplateIndex = idx
                                    activePanel = null
                                },
                                onDismiss = { activePanel = null }
                            )
                            Panel.TextSize -> TextSizePanel(
                                currentSize = fullSettings.fontSize,
                                onSizeChange = { uiChange { fullSettings = fullSettings.copy(fontSize = it) } },
                                onReset = { uiChange { fullSettings = fullSettings.copy(fontSize = Defaults.FULL_SETTINGS.fontSize) } },
                                onDismiss = { activePanel = null }
                            )
                            Panel.Outline -> OutlinePanel(
                                enabled = fullSettings.enableOutline,
                                onToggle = { uiChange { fullSettings = fullSettings.copy(enableOutline = it, outlineWidth = 2f) } },
                                color = fullSettings.outlineColor,
                                onColorChange = { uiChange { fullSettings = fullSettings.copy(outlineColor = it) } },
                                onResetAll = { uiChange { fullSettings = fullSettings.copy(enableOutline = true, outlineWidth = 2f, outlineColor = Defaults.FULL_SETTINGS.outlineColor) } },
                                onResetColor = { uiChange { fullSettings = fullSettings.copy(outlineColor = Defaults.FULL_SETTINGS.outlineColor) } },
                                onDismiss = { activePanel = null }
                            )
                            Panel.Shadow -> ShadowPanel(
                                enabled = fullSettings.enableShadow,
                                onToggle = { uiChange { fullSettings = fullSettings.copy(enableShadow = it) } },
                                blur = fullSettings.shadowBlur,
                                onBlurChange = { uiChange { fullSettings = fullSettings.copy(shadowBlur = it) } },
                                offsetX = fullSettings.shadowOffsetX,
                                onOffsetXChange = { uiChange { fullSettings = fullSettings.copy(shadowOffsetX = it) } },
                                offsetY = fullSettings.shadowOffsetY,
                                onOffsetYChange = { uiChange { fullSettings = fullSettings.copy(shadowOffsetY = it) } },
                                color = fullSettings.shadowColor,
                                onColorChange = { uiChange { fullSettings = fullSettings.copy(shadowColor = it) } },
                                onResetAll = { uiChange { fullSettings = fullSettings.copy(enableShadow = false, shadowBlur = Defaults.FULL_SETTINGS.shadowBlur, shadowOffsetX = Defaults.FULL_SETTINGS.shadowOffsetX, shadowOffsetY = Defaults.FULL_SETTINGS.shadowOffsetY, shadowColor = Defaults.FULL_SETTINGS.shadowColor) } },
                                onResetBlur = { uiChange { fullSettings = fullSettings.copy(shadowBlur = Defaults.FULL_SETTINGS.shadowBlur) } },
                                onResetOffsetX = { uiChange { fullSettings = fullSettings.copy(shadowOffsetX = Defaults.FULL_SETTINGS.shadowOffsetX) } },
                                onResetOffsetY = { uiChange { fullSettings = fullSettings.copy(shadowOffsetY = Defaults.FULL_SETTINGS.shadowOffsetY) } },
                                onResetColor = { uiChange { fullSettings = fullSettings.copy(shadowColor = Defaults.FULL_SETTINGS.shadowColor) } },
                                onDismiss = { activePanel = null }
                            )
                            Panel.Bg -> BgPanel(
                                bgColor = fullSettings.backgroundColor,
                                onColorChange = { uiChange { fullSettings = fullSettings.copy(backgroundColor = it) } },
                                onReset = { uiChange { fullSettings = fullSettings.copy(backgroundColor = Defaults.FULL_SETTINGS.backgroundColor) } },
                                onDismiss = { activePanel = null }
                            )
                            Panel.FontColor -> FontColorPanel(
                                currentColor = fullSettings.fontColor,
                                onColorChange = { uiChange { fullSettings = fullSettings.copy(fontColor = it) } },
                                onReset = { uiChange { fullSettings = fullSettings.copy(fontColor = Defaults.FULL_SETTINGS.fontColor) } },
                                onDismiss = { activePanel = null }
                            )
                            Panel.FontFamily -> FontFamilyPanel(
                                currentFont = fullSettings.fontFamily,
                                onFontChange = { uiChange { fullSettings = fullSettings.copy(fontFamily = it) } },
                                onDismiss = { activePanel = null }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialogs
    if (showCloseConfirm) {
        ConfirmationOverlay(
            title = stringResource(R.string.subtitle_unsaved_changes_title),
            message = stringResource(R.string.subtitle_unsaved_changes_message),
            confirmLabel = stringResource(R.string.subtitle_save_close),
            onConfirm = {
                commitDrag(); onSave(); hasChanges = false; showCloseConfirm = false; onDismiss()
            },
            dismissLabel = stringResource(R.string.subtitle_discard),
            onDismiss = { showCloseConfirm = false; onDismiss() },
            cancelLabel = stringResource(R.string.subtitle_cancel),
            onCancel = { showCloseConfirm = false }
        )
    }

    if (showResetConfirm) {
        ConfirmationOverlay(
            title = stringResource(R.string.subtitle_reset_settings_title),
            message = stringResource(R.string.subtitle_reset_settings_message),
            confirmLabel = stringResource(R.string.subtitle_reset),
            onConfirm = {
                fullSettings = Defaults.FULL_SETTINGS; showResetConfirm = false
            },
            onDismiss = { showResetConfirm = false }
        )
    }

    if (showRenameDialog) {
        RenameOverlay(
            currentName = renameText,
            onNameChange = { renameText = it },
            onConfirm = {
                if (renameText.isNotBlank()) onRenameProfile(activeProfileIndex, renameText.trim())
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false }
        )
    }
}


private enum class Panel {
    Template, TextSize, Outline, Shadow, Bg, FontColor, FontFamily
}

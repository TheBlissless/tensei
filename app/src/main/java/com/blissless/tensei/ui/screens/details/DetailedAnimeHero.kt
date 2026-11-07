package com.blissless.tensei.ui.screens.details

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.blissless.tensei.util.LocaleDate
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.blissless.tensei.R
import com.blissless.tensei.ui.components.DetailSpecGridSkeleton
import com.blissless.tensei.ui.components.DetailStatStripSkeleton
import com.blissless.tensei.ui.components.anilistAnnotated
import com.blissless.tensei.ui.components.cleanAniListText
import com.blissless.tensei.ui.theme.tenseiColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Hero-section components of the anime detail screen: stat/spec cells and
// the watch-now button.



// ─── Data classes ──────────────────────────────────────────────────────────

internal data class SpecEntry(
    val label: String,
    val value: String,
    val icon: ImageVector? = null,
    val fullSpan: Boolean = false
)


// ─── Composables ───────────────────────────────────────────────────────────

/**
 * A single stat cell used in the hero section: large value on top, small
 * uppercase label below. Optionally prefixed with an icon.
 */
@Composable
internal fun HeroStatCell(
    value: String,
    label: String,
    accent: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
        )
    }
}


/**
 * A bento-style spec cell: small uppercase label on top, value (with optional
 * icon) below. Used in the spec grid on the details screen.
 */
@Composable
internal fun BentoSpecCell(spec: SpecEntry, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f))
            .padding(14.dp)
    ) {
        Text(
            spec.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            spec.icon?.let { ic ->
                Icon(
                    ic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                spec.value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}


/**
 * "Watch Now" button card shown on the detailed anime screen.
 *
 * Displays a prominent play button that opens the episode selection
 * dialog. Disabled when the anime hasn't aired yet.
 *
 * @param status            The anime's airing status (e.g. "RELEASING", "NOT_YET_RELEASED")
 * @param streamMethod      The configured stream method ("magnet", "direct", etc.)
 * @param hasDefaultMagnetExt Whether a default magnet (torrent) extension is configured
 * @param hasDefaultStreamExt Whether a default stream extension is configured
 * @param hasDefaultExtPkg Whether a default extension package is configured
 * @param onNoDefaultExtension Called when no default extension is set
 * @param onShowEpisodeSelection Called when the button is clicked and an extension is configured
 */
@Composable
internal fun WatchNowButton(
    status: String?,
    streamMethod: String,
    hasDefaultMagnetExt: Boolean,
    hasDefaultStreamExt: Boolean,
    hasDefaultExtPkg: Boolean,
    onNoDefaultExtension: () -> Unit,
    onShowEpisodeSelection: () -> Unit,
) {
    val notYetAired = status == "NOT_YET_RELEASED"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Button(
                onClick = {
                    val hasDefault =
                        streamMethod == "magnet" && (hasDefaultMagnetExt || hasDefaultStreamExt) ||
                        streamMethod == "direct" && hasDefaultExtPkg
                    if (!hasDefault) {
                        onNoDefaultExtension()
                    } else {
                        onShowEpisodeSelection()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !notYetAired,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            ) {
                Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.anime_detail_watch_now), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

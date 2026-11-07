package com.blissless.tensei.ui.screens.manga

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents            
import androidx.compose.material.icons.filled.Favorite                
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.blissless.tensei.ui.components.RelatedMediaCard
import com.blissless.tensei.ui.components.SkeletonPill
import com.blissless.tensei.ui.components.SkeletonTextLine
import com.blissless.tensei.ui.components.asRelatedMedia
import com.blissless.tensei.ui.components.BannerOverlayText
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.blissless.tensei.R
import com.blissless.tensei.data.models.MangaCharacterNode
import com.blissless.tensei.data.models.MangaDetail
import com.blissless.tensei.data.models.MangaMedia
import com.blissless.tensei.data.models.MangaRelation
import com.blissless.tensei.data.models.MangaStaffEdge
import com.blissless.tensei.data.models.MangaRankingEntry
import com.blissless.tensei.ui.screens.details.easeOut
import com.blissless.tensei.ui.theme.statusAccentFor
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.tenseiColors
import com.blissless.tensei.ui.theme.MangaStatusLabels
import com.blissless.tensei.ui.dialogs.userScoreToDisplay
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * The large content sections of the manga detail screen's LazyColumn —
 * extracted verbatim from [DetailedMangaScreen] so each section (header,
 * progress row, status/tracking card, relations, recommendations,
 * characters and staff rails, rankings) reads and evolves independently.
 */

@Composable
internal fun MangaDetailHeaderItem(
    alpha: Float,
    displayData: com.blissless.tensei.data.models.MangaDetail,
    formatDisplay: String,
    statusDisplay: String,
    isLoading: Boolean,
    resolvedChapterCount: Int?,
    onShowFullscreenImage: (String?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .width(140.dp).height(200.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { onShowFullscreenImage(displayData.cover) },
                                shape = RoundedCornerShape(16.dp),
                                shadowElevation = 16.dp,
                                color = Color.Transparent
                            ) {
                                AsyncImage(
                                    model = displayData.cover, contentDescription = displayData.title,
                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clickable {
                                        onShowFullscreenImage(displayData.cover)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Manga Title", displayData.title))
                        }.padding(vertical = 4.dp)) {
                            BannerOverlayText(
                                // The title overlaps the banner, so it gets an outline in the
                                // theme's background colour to stay readable on the image.
                                text = displayData.title, style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold, maxLines = 10, overflow = TextOverflow.Clip,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.padding(start = 4.dp).size(16.dp).background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f), CircleShape).padding(3.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!displayData.titleEnglish.isNullOrEmpty() && displayData.titleEnglish != displayData.title) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Manga Title", displayData.titleEnglish))
                            }.padding(vertical = 4.dp)) {
                                BannerOverlayText(
                                    text = displayData.titleEnglish, style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 10, overflow = TextOverflow.Clip,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        if (!displayData.titleNative.isNullOrEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Manga Title", displayData.titleNative))
                            }.padding(vertical = 4.dp)) {
                                BannerOverlayText(
                                    text = displayData.titleNative, style = MaterialTheme.typography.bodySmall,
                                    maxLines = 10, overflow = TextOverflow.Clip,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            displayData.averageScore?.let { score ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.background(tenseiColors.rating.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Star, null, tint = tenseiColors.rating, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        String.format(Locale.US, "%.1f", score / 10.0),
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = tenseiColors.rating
                                    )
                                }
                            }
                            if (displayData.averageScore == null && isLoading) {
                                SkeletonPill(width = 68.dp, height = 30.dp)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    displayData.status != null -> statusAccentFor(displayData.status).copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                }
                            ) {
                                Text(
                                    statusDisplay, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        displayData.status != null -> statusAccentFor(displayData.status)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (displayData.year != null) {
                                Text(displayData.year.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (displayData.year != null && displayData.format != null) {
                                Text("\u2022", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            }
                            displayData.format?.let {
                                Text(formatDisplay, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            val chapterCount = resolvedChapterCount
                            if (chapterCount != null) {
                                Text("\u2022", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Text(stringResource(R.string.manga_detail_chapters_short, chapterCount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (displayData.year == null && displayData.format == null && resolvedChapterCount == null && isLoading) {
                                SkeletonTextLine(width = 96.dp, height = 11.dp)
                            }
                        }
                    }
                }
}

@Composable
internal fun MangaDetailProgressItem(
    onStartReader: (chapterIndex: Int) -> Unit,
    alpha: Float,
    liveProgress: Int,
) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        Button(
                            onClick = {
                                onStartReader(-1)
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (liveProgress > 0) stringResource(R.string.manga_detail_read_now) else stringResource(R.string.manga_detail_start_reading),
                                fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
}

@Composable
internal fun MangaDetailStatusItem(
    manga: com.blissless.tensei.data.models.MangaMedia,
    alpha: Float,
    isMangaFavorited: Boolean,
    liveScore: Int?,
    onFavoriteClick: () -> Unit,
    statusProgress: Int,
    statusToCheck: String?,
    totalCh: Int,
    onShowStatusDialogChange: (Boolean) -> Unit,
    onShowRatingSheetChange: (Boolean) -> Unit,
) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(Icons.Outlined.BookmarkAdd, contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(R.string.manga_detail_add_to_list),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            if (statusToCheck != null || liveScore != null) {
                                val listStatusColor = statusColor(statusToCheck)
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (liveScore != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = tenseiColors.rating
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "${userScoreToDisplay(liveScore)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = tenseiColors.rating
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        if (statusToCheck != null) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = listStatusColor.copy(alpha = 0.12f)) {
                                                Text(
                                                    text = MangaStatusLabels[statusToCheck] ?: statusToCheck,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = listStatusColor, fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (totalCh > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // A COMPLETED manga is fully read even if the tracker has a stale 0 progress
                                        val completedProgress = if (statusToCheck == "COMPLETED") totalCh else statusProgress.coerceAtLeast(0)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = "$completedProgress", style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(text = " / $totalCh", style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        if (statusToCheck != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onShowStatusDialogChange(true) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.manga_detail_change), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onShowRatingSheetChange(true) },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (liveScore != null && liveScore > 0) tenseiColors.rating.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (liveScore != null && liveScore > 0) tenseiColors.rating else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (liveScore != null && liveScore > 0) tenseiColors.rating else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = stringResource(R.string.manga_detail_rate),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = onFavoriteClick,
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isMangaFavorited) tenseiColors.favorite.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isMangaFavorited) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.5.dp,
                                        if (isMangaFavorited) tenseiColors.favorite else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        if (isMangaFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        null, Modifier.size(20.dp),
                                        tint = if (isMangaFavorited) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = { onShowStatusDialogChange(true) },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Icon(Icons.Outlined.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.manga_detail_add_to_list), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = onFavoriteClick,
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isMangaFavorited) tenseiColors.favorite.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isMangaFavorited) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(1.5.dp,
                                        if (isMangaFavorited) tenseiColors.favorite else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        if (isMangaFavorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        null, Modifier.size(20.dp),
                                        tint = if (isMangaFavorited) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
}

@Composable
internal fun MangaDetailRelationsItem(
    onRelationClick: (com.blissless.tensei.data.models.MangaRelation) -> Unit,
    onViewAllRelations: () -> Unit,
    preferEnglishTitles: Boolean,
    relations: List<com.blissless.tensei.data.models.MangaRelation>,
    alpha: Float,
) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.manga_detail_relations), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.manga_detail_connected_series), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onViewAllRelations) {
                                    Text(stringResource(R.string.manga_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                itemsIndexed(items = relations, key = { _, r -> r.id }) { _, relation ->
                                    Column(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .clickable { onRelationClick(relation) }
                                            .padding(4.dp)
                                    ) {
                                        RelatedMediaCard(
                                            media = relation.asRelatedMedia(),
                                            preferEnglishTitle = preferEnglishTitles,
                                            titleHeight = 32.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
}

@Composable
internal fun MangaDetailRecommendationsItem(
    onViewAllRecommendations: () -> Unit,
    navigateToMangaDetail: (Int) -> Unit,
    preferEnglishTitles: Boolean,
    recommendations: List<com.blissless.tensei.data.models.MangaMedia>,
    alpha: Float,
    density: androidx.compose.ui.unit.Density,
    slideOffset: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.manga_detail_recommendations), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.manga_detail_you_might_also_enjoy), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onViewAllRecommendations) {
                                    Text(stringResource(R.string.manga_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val recListState = rememberLazyListState()
                            val isRecScrolling by remember {
                                derivedStateOf { recListState.isScrollInProgress }
                            }
                            val recCameraDistancePx = with(density) { 12.dp.toPx() }
                            val recIntro = ((1000f - slideOffset.value) / 1000f).coerceIn(0f, 1f)

                            LazyRow(
                                state = recListState,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = recommendations.take(20),
                                    key = { _, rec -> rec.id }
                                ) { index, rec ->
                                    val recCenterOffset = com.blissless.tensei.ui.components.rememberRailItemCenterOffset(
                                        listState = recListState,
                                        index = index,
                                    )

                                    val recAnimatedOffset = com.blissless.tensei.ui.components.animateRailItemOffset(
                                        centerOffset = recCenterOffset,
                                        isScrolling = isRecScrolling,
                                        label = "recCenterOffset",
                                    )

                                    val railTransforms = com.blissless.tensei.ui.components.railScrollTransforms(recAnimatedOffset)
                                    val recScrollScale = railTransforms.scale
                                    val recScrollAlpha = railTransforms.alpha
                                    val recScrollTranslationX = railTransforms.translationX
                                    val recScrollRotationY = railTransforms.rotationY

                                    val recIndexFloat = index.toFloat()
                                    val recStaggeredProgress = ((recIntro * 1000f - (recIndexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                    val recEasedProgress = easeOut(recStaggeredProgress)
                                    val recIntroScale = if (recIntro >= 1f) 1f else 0.85f + recEasedProgress * 0.15f
                                    val recIntroAlpha = if (recIntro >= 1f) 1f else recEasedProgress

                                    Column(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .clickable { navigateToMangaDetail(rec.id) }
                                            .graphicsLayer {
                                                scaleX = recIntroScale * recScrollScale
                                                scaleY = recIntroScale * recScrollScale
                                                this.alpha = recIntroAlpha * recScrollAlpha
                                                translationX = recScrollTranslationX
                                                rotationY = recScrollRotationY
                                                cameraDistance = recCameraDistancePx
                                            }
                                            .padding(4.dp)
                                    ) {
                                        RelatedMediaCard(
                                            media = rec.asRelatedMedia(),
                                            preferEnglishTitle = preferEnglishTitles,
                                            titleHeight = 32.dp,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
}

@Composable
internal fun MangaDetailCharactersItem(
    onCharacterClick: (Int) -> Unit,
    onViewAllCharacters: () -> Unit,
    characters: List<com.blissless.tensei.data.models.MangaCharacterNode>,
    alpha: Float,
    density: androidx.compose.ui.unit.Density,
    slideOffset: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.manga_detail_characters), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.manga_detail_appearing_characters), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onViewAllCharacters) {
                                    Text(stringResource(R.string.manga_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val charListState = rememberLazyListState()
                            val isCharScrolling by remember {
                                derivedStateOf { charListState.isScrollInProgress }
                            }
                            val charCameraDistancePx = with(density) { 12.dp.toPx() }
                            val charIntro = ((1000f - slideOffset.value) / 1000f).coerceIn(0f, 1f)

                            LazyRow(
                                state = charListState,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(characters, key = { _, c -> c.id }) { index, character ->
                                    val charCenterOffset = com.blissless.tensei.ui.components.rememberRailItemCenterOffset(
                                        listState = charListState,
                                        index = index,
                                    )

                                    val charAnimatedOffset = com.blissless.tensei.ui.components.animateRailItemOffset(
                                        centerOffset = charCenterOffset,
                                        isScrolling = isCharScrolling,
                                        label = "charCenterOffset",
                                    )

                                    val railTransforms = com.blissless.tensei.ui.components.railScrollTransforms(charAnimatedOffset)
                                    val charScrollScale = railTransforms.scale
                                    val charScrollAlpha = railTransforms.alpha
                                    val charScrollTranslationX = railTransforms.translationX
                                    val charScrollRotationY = railTransforms.rotationY

                                    val charIndexFloat = index.toFloat()
                                    val charStaggeredProgress = ((charIntro * 1000f - (charIndexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                    val charEasedProgress = easeOut(charStaggeredProgress)
                                    val charIntroScale = if (charIntro >= 1f) 1f else 0.85f + charEasedProgress * 0.15f
                                    val charIntroAlpha = if (charIntro >= 1f) 1f else charEasedProgress

                                    Column(
                                        modifier = Modifier.width(80.dp).clip(RoundedCornerShape(12.dp))
                                            .graphicsLayer {
                                                scaleX = charIntroScale * charScrollScale
                                                scaleY = charIntroScale * charScrollScale
                                                this.alpha = charIntroAlpha * charScrollAlpha
                                                translationX = charScrollTranslationX
                                                rotationY = charScrollRotationY
                                                cameraDistance = charCameraDistancePx
                                            }
                                            .clickable { character.id.let(onCharacterClick) }.padding(4.dp)
                                    ) {
                                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                            Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                                            ) {
                                                AsyncImage(model = character.image?.large ?: "", contentDescription = character.name?.full,
                                                    contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(character.name?.full ?: stringResource(R.string.manga_detail_unknown), style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.height(28.dp))
                                    }
                                }
                            }
                        }
                    }
}

@Composable
internal fun MangaDetailStaffItem(
    onStaffClick: (Int) -> Unit,
    onViewAllStaff: () -> Unit,
    staff: List<com.blissless.tensei.data.models.MangaStaffEdge>,
    alpha: Float,
    density: androidx.compose.ui.unit.Density,
    slideOffset: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.manga_detail_staff), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.manga_detail_production_crew), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = onViewAllStaff) {
                                    Text(stringResource(R.string.manga_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            val staffListState = rememberLazyListState()
                            val isStaffScrolling by remember {
                                derivedStateOf { staffListState.isScrollInProgress }
                            }
                            val staffCameraDistancePx = with(density) { 12.dp.toPx() }
                            val staffIntro = ((1000f - slideOffset.value) / 1000f).coerceIn(0f, 1f)

                            LazyRow(
                                state = staffListState,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(staff, key = { index, _ -> "staff_$index" }) { index, staffEdge ->
                                    staffEdge.node?.let { staffNode ->
                                        val staffCenterOffset = com.blissless.tensei.ui.components.rememberRailItemCenterOffset(
                                            listState = staffListState,
                                            index = index,
                                        )

                                        val staffAnimatedOffset = com.blissless.tensei.ui.components.animateRailItemOffset(
                                            centerOffset = staffCenterOffset,
                                            isScrolling = isStaffScrolling,
                                            label = "mangaStaffCenterOffset",
                                        )

                                        val railTransforms = com.blissless.tensei.ui.components.railScrollTransforms(staffAnimatedOffset)
                                        val staffScrollScale = railTransforms.scale
                                        val staffScrollAlpha = railTransforms.alpha
                                        val staffScrollTranslationX = railTransforms.translationX
                                        val staffScrollRotationY = railTransforms.rotationY

                                        val staffIndexFloat = index.toFloat()
                                        val staffStaggeredProgress = ((staffIntro * 1000f - (staffIndexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                        val staffEasedProgress = easeOut(staffStaggeredProgress)
                                        val staffIntroScale = if (staffIntro >= 1f) 1f else 0.85f + staffEasedProgress * 0.15f
                                        val staffIntroAlpha = if (staffIntro >= 1f) 1f else staffEasedProgress

                                        Column(
                                            modifier = Modifier.width(80.dp).clip(RoundedCornerShape(12.dp))
                                                .clickable { staffNode.id.let(onStaffClick) }
                                                .graphicsLayer {
                                                    scaleX = staffIntroScale * staffScrollScale
                                                    scaleY = staffIntroScale * staffScrollScale
                                                    this.alpha = staffIntroAlpha * staffScrollAlpha
                                                    translationX = staffScrollTranslationX
                                                    rotationY = staffScrollRotationY
                                                    cameraDistance = staffCameraDistancePx
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                                                Card(shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxSize(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                                                ) {
                                                    AsyncImage(model = staffNode.image?.large ?: "", contentDescription = staffNode.name?.full,
                                                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(staffNode.name?.full ?: stringResource(R.string.manga_detail_unknown), style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.height(28.dp))
                                            staffEdge.role?.let { role ->
                                                Text(role.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelSmall, maxLines = 1,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
}

@Composable
internal fun MangaDetailRankingsItem(
    rankings: List<com.blissless.tensei.data.models.MangaRankingEntry>,
    alpha: Float,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(stringResource(R.string.manga_detail_rankings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.manga_detail_community_performance), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            rankings.forEachIndexed { index, ranking ->
                                val rankingTypeLabel = when (ranking.type) {
                                    "RATED" -> stringResource(R.string.manga_detail_highest_rated)
                                    "POPULAR" -> stringResource(R.string.manga_detail_most_popular)
                                    else -> ranking.type ?: stringResource(R.string.manga_detail_ranking)
                                }
                                val rawContext = ranking.context?.trim()?.uppercase()
                                val rankingContextLabel = when {
                                    rawContext?.contains("ALL TIME") == true -> stringResource(R.string.manga_detail_all_time)
                                    rawContext?.startsWith("SEASON") == true ->
                                        rawContext.removePrefix("SEASON").trim().ifBlank { stringResource(R.string.manga_detail_seasonal) }
                                            .replaceFirstChar { it.uppercaseChar() }
                                    rawContext?.startsWith("YEAR") == true ->
                                        rawContext.removePrefix("YEAR").trim().ifBlank { stringResource(R.string.manga_detail_this_year) }
                                    rawContext?.startsWith("DECADE") == true ->
                                        rawContext.removePrefix("DECADE").trim().ifBlank { stringResource(R.string.manga_detail_this_decade) }
                                    rawContext?.startsWith("WEEK") == true ->
                                        rawContext.removePrefix("WEEK").trim().ifBlank { stringResource(R.string.manga_detail_this_week) }
                                    ranking.allTime == true -> stringResource(R.string.manga_detail_all_time)
                                    ranking.season != null && ranking.year != null ->
                                        "${ranking.season.lowercase().replaceFirstChar { it.uppercaseChar() }} ${ranking.year}"
                                    ranking.year != null -> ranking.year.toString()
                                    ranking.season != null -> ranking.season.lowercase().replaceFirstChar { it.uppercaseChar() }
                                    !rawContext.isNullOrBlank() -> rawContext.lowercase().replaceFirstChar { it.uppercaseChar() }
                                    else -> stringResource(R.string.manga_detail_seasonal)
                                }
                                val isAllTime = ranking.allTime == true
                                val allTimeAccent = tenseiColors.rating
                                val allTimeRankText = if (tenseiColors.monochrome) 
                                    MaterialTheme.colorScheme.onSurface else Color(0xFF1a1205)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                            .background(
                                                if (isAllTime && !tenseiColors.monochrome) {
                                                    Brush.linearGradient(listOf(Color(0xFFd97706), Color(0xFFfbbf24)))
                                                } else if (isAllTime) {
                                                    Brush.linearGradient(listOf(
                                                        allTimeAccent.copy(alpha = 0.18f),
                                                        allTimeAccent.copy(alpha = 0.18f)
                                                    ))
                                                } else {
                                                    Brush.linearGradient(listOf(
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                    ))
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "#${ranking.rank ?: "-"}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (isAllTime) allTimeRankText else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            rankingTypeLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isAllTime) {
                                                Icon(
                                                    imageVector = Icons.Filled.EmojiEvents,
                                                    contentDescription = null,
                                                    tint = allTimeAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                rankingContextLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isAllTime) allTimeAccent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                if (index != rankings.lastIndex) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
}


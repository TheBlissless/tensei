package com.blissless.tensei.ui.screens.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.blissless.tensei.viewmodel.MainViewModel
import com.blissless.tensei.R
import com.blissless.tensei.data.models.AnimeRelation
import com.blissless.tensei.data.models.DetailedAnimeData
import com.blissless.tensei.data.models.ExploreAnime
import com.blissless.tensei.ui.dialogs.userScoreToDisplay
import com.blissless.tensei.ui.components.RelatedMediaCard
import com.blissless.tensei.ui.components.SkeletonPill
import com.blissless.tensei.ui.components.SkeletonTextLine
import com.blissless.tensei.ui.components.StaffEdgeJobText
import com.blissless.tensei.ui.components.asRelatedMedia
import com.blissless.tensei.ui.components.BannerOverlayText
import com.blissless.tensei.ui.theme.StatusLabels
import com.blissless.tensei.ui.theme.statusAccentFor
import com.blissless.tensei.ui.theme.statusColor
import com.blissless.tensei.ui.theme.tenseiColors
import java.util.Locale
import kotlin.math.absoluteValue

/**
 * The large content sections of the anime detail screen's LazyColumn —
 * extracted verbatim from [DetailedAnimeScreen] so each section
 * (header/status card/relations/recommendations/characters/staff rails)
 * reads and evolves independently.
 */

@Composable
internal fun AnimeDetailHeaderItem(
    initialCardBounds: MainViewModel.CardBounds?,
    alpha: Float,
    coverAnimationProgress: androidx.compose.animation.core.Animatable<Float, androidx.compose.animation.core.AnimationVector1D>,
    displayData: com.blissless.tensei.data.models.DetailedAnimeData,
    formatDisplay: String,
    statusDisplay: String,
    isLoadingDetails: Boolean,
    onShowFullscreenImage: (String?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box {
                        val cardWidth = 140.dp
                        val cardHeight = 200.dp
                        val targetX = 0.dp
                        val targetY = 0.dp

                        Box(
                            modifier = Modifier
                                .width(cardWidth)
                                .height(cardHeight)
                                .zIndex(if (initialCardBounds != null && coverAnimationProgress.value < 1f) 100f else 0f)
                                .graphicsLayer {
                                    if (initialCardBounds != null) {
                                        val progress = FastOutSlowInEasing.transform(coverAnimationProgress.value)
                                        val startX = initialCardBounds.bounds.left
                                        val startY = initialCardBounds.bounds.top
                                        val startWidth = initialCardBounds.bounds.width()
                                        val startHeight = initialCardBounds.bounds.height()

                                        val currentWidth = startWidth + (cardWidth.toPx() - startWidth) * progress
                                        val currentHeight = startHeight + (cardHeight.toPx() - startHeight) * progress
                                        val currentX = startX + (targetX.toPx() - startX) * progress
                                        val currentY = startY + (targetY.toPx() - startY) * progress

                                        scaleX = currentWidth / size.width
                                        scaleY = currentHeight / size.height
                                        translationX = currentX
                                        translationY = currentY
                                    }
                                }
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
                            clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.anime_detail_clip_anime_title), displayData.title))
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
                                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.anime_detail_clip_anime_title), displayData.titleEnglish))
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
                                clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.anime_detail_clip_anime_title), displayData.titleNative))
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
                            if (displayData.averageScore == null && isLoadingDetails) {
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
                                Text(
                                    displayData.year.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (displayData.year != null && displayData.format != null) {
                                Text(
                                    "\u2022",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            displayData.format?.let {
                                Text(
                                    formatDisplay,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val episodeCount = displayData.episodes.takeIf { it > 0 }
                            if (episodeCount != null) {
                                Text(
                                    "\u2022",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Text(
                                    stringResource(R.string.anime_detail_eps_count, episodeCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (displayData.year == null && displayData.format == null && episodeCount == null && isLoadingDetails) {
                                SkeletonTextLine(width = 96.dp, height = 11.dp)
                            }
                        }
                    }
                }
}

@Composable
internal fun AnimeDetailStatusItem(
    anime: com.blissless.tensei.data.models.DetailedAnimeData,
    isLoggedIn: Boolean,
    alpha: Float,
    effectiveIsFavorite: Boolean,
    effectiveUserScore: Int?,
    onFavoriteClick: () -> Unit,
    statusProgress: Int,
    statusToCheck: String?,
    onShowStatusDialogChange: (Boolean) -> Unit,
    onShowRatingSheetChange: (Boolean) -> Unit,
) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val totalEps = anime.episodes.takeIf { it > 0 } ?: anime.episodes

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
                                        Icon(
                                            Icons.Outlined.BookmarkAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    if (isLoggedIn) stringResource(R.string.anime_detail_add_to_list) else stringResource(R.string.anime_detail_local_list),
                                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (!isLoggedIn) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.anime_detail_offline),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (statusToCheck != null || effectiveUserScore != null) {
                                val listStatusColor = statusColor(statusToCheck)
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (effectiveUserScore != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.Star,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp),
                                                    tint = tenseiColors.rating
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = "${userScoreToDisplay(effectiveUserScore)}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = tenseiColors.rating
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                        }
                                        if (statusToCheck != null) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = listStatusColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = StatusLabels[statusToCheck] ?: statusToCheck,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = listStatusColor,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (totalEps > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$statusProgress",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = " / $totalEps",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Light,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
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
                                     Text(stringResource(R.string.anime_detail_change), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { onShowRatingSheetChange(true) },
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (effectiveUserScore != null && effectiveUserScore > 0) tenseiColors.rating.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (effectiveUserScore != null && effectiveUserScore > 0) tenseiColors.rating else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (effectiveUserScore != null && effectiveUserScore > 0) tenseiColors.rating else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = stringResource(R.string.anime_detail_rate),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = onFavoriteClick,
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (effectiveIsFavorite) tenseiColors.favorite.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (effectiveIsFavorite) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (effectiveIsFavorite) tenseiColors.favorite else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        if (effectiveIsFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        null,
                                        Modifier.size(20.dp),
                                        tint = if (effectiveIsFavorite) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
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
                                    Text(stringResource(R.string.anime_detail_add_to_list), fontWeight = FontWeight.SemiBold)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                OutlinedButton(
                                    onClick = onFavoriteClick,
                                    modifier = Modifier.height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (effectiveIsFavorite) tenseiColors.favorite.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (effectiveIsFavorite) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
                                    ),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (effectiveIsFavorite) tenseiColors.favorite else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Icon(
                                        if (effectiveIsFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        null,
                                        Modifier.size(20.dp),
                                        tint = if (effectiveIsFavorite) tenseiColors.favorite else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
}

@Composable
internal fun AnimeDetailRelationsItem(
    displayData: com.blissless.tensei.data.models.DetailedAnimeData,
    alpha: Float,
    cinematicProgress: Float,
    density: androidx.compose.ui.unit.Density,
    preferEnglishTitles: Boolean,
    onRelationClick: (com.blissless.tensei.data.models.AnimeRelation) -> Unit,
    onViewAllRelations: (Int, String, String?) -> Unit,
) {
    val filteredRelations = displayData.relations
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
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
                                    Icon(
                                        Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.anime_detail_relations),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(R.string.anime_detail_connected_series),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (filteredRelations.isNotEmpty()) {
                                    TextButton(onClick = { 
                                        onViewAllRelations(displayData.id, displayData.title, displayData.titleEnglish) 
                                    }) {
                                        Text(stringResource(R.string.anime_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val relationListState = rememberLazyListState()
                            val isRelationScrolling by remember {
                                derivedStateOf { relationListState.isScrollInProgress }
                            }
                            val cameraDistancePx = with(density) { 12.dp.toPx() }

                            LazyRow(
                                state = relationListState,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = filteredRelations,
                                    key = { _, relation -> relation.id }
                                ) { index, relation ->
                                    val centerOffset = com.blissless.tensei.ui.components.rememberRailItemCenterOffset(
                                        listState = relationListState,
                                        index = index,
                                    )

                                    val animatedOffset = com.blissless.tensei.ui.components.animateRailItemOffset(
                                        centerOffset = centerOffset,
                                        isScrolling = isRelationScrolling,
                                        label = "relationCenterOffset",
                                    )

                                    val railTransforms = com.blissless.tensei.ui.components.railScrollTransforms(animatedOffset)
                                    val scrollScale = railTransforms.scale
                                    val scrollAlpha = railTransforms.alpha
                                    val scrollTranslationX = railTransforms.translationX
                                    val scrollRotationY = railTransforms.rotationY

                                    val indexFloat = index.toFloat()
                                    val staggeredProgress = ((cinematicProgress * 1000f - (indexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                    val easedProgress = easeOut(staggeredProgress)

                                    val introScale = if (cinematicProgress >= 1f) 1f else 0.85f + easedProgress * 0.15f
                                    val introAlpha = if (cinematicProgress >= 1f) 1f else easedProgress

                                    Column(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .graphicsLayer {
                                                scaleX = introScale * scrollScale
                                                scaleY = introScale * scrollScale
                                                this.alpha = introAlpha * scrollAlpha
                                                translationX = scrollTranslationX
                                                rotationY = scrollRotationY
                                                cameraDistance = cameraDistancePx
                                            }
                                            .clickable {
                                                onRelationClick(relation)
                                            }
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
internal fun AnimeDetailRecommendationsItem(
    displayData: com.blissless.tensei.data.models.DetailedAnimeData,
    alpha: Float,
    cinematicProgress: Float,
    density: androidx.compose.ui.unit.Density,
    preferEnglishTitles: Boolean,
    onRecommendationClick: (com.blissless.tensei.data.models.ExploreAnime) -> Unit,
    onViewAllRecommendations: (Int, String, String?) -> Unit,
) {
    val recommendations = displayData.recommendations
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
                                    Text(stringResource(R.string.anime_detail_recommendations), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.anime_detail_you_might_also_enjoy), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), letterSpacing = 0.5.sp)
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { onViewAllRecommendations(displayData.id, displayData.title, displayData.titleEnglish) }) {
                                    Text(stringResource(R.string.anime_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
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
                                    val recStaggeredProgress = ((cinematicProgress * 1000f - (recIndexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                    val recEasedProgress = easeOut(recStaggeredProgress)

                                    val recIntroScale = if (cinematicProgress >= 1f) 1f else 0.85f + recEasedProgress * 0.15f
                                    val recIntroAlpha = if (cinematicProgress >= 1f) 1f else recEasedProgress

                                    Column(
                                        modifier = Modifier
                                            .width(110.dp)
                                            .graphicsLayer {
                                                scaleX = recIntroScale * recScrollScale
                                                scaleY = recIntroScale * recScrollScale
                                                this.alpha = recIntroAlpha * recScrollAlpha
                                                translationX = recScrollTranslationX
                                                rotationY = recScrollRotationY
                                                cameraDistance = recCameraDistancePx
                                            }
                                            .clickable {
                                                onRecommendationClick(rec)
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
internal fun AnimeDetailCharactersItem(
    castList: List<com.blissless.tensei.data.models.DetailedAnimeCharacterNode>,
    alpha: Float,
    cinematicProgress: Float,
    density: androidx.compose.ui.unit.Density,
    onCharacterClick: (Int) -> Unit,
    onViewAllCast: () -> Unit,
) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
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
                                    Icon(
                                        Icons.Default.Group,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.anime_detail_cast),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(R.string.anime_detail_characters),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (castList.isNotEmpty()) {
                                    TextButton(onClick = onViewAllCast) {
                                        Text(stringResource(R.string.anime_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val castListState = rememberLazyListState()
                            val isCastScrolling by remember {
                                derivedStateOf { castListState.isScrollInProgress }
                            }
                            val cameraDistancePx = with(density) { 12.dp.toPx() }

                            LazyRow(
                                state = castListState,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = castList,
                                    key = { _, character -> character.id }
                                ) { index, character ->
                                    val centerOffset = com.blissless.tensei.ui.components.rememberRailItemCenterOffset(
                                        listState = castListState,
                                        index = index,
                                    )

                                    val animatedOffset = com.blissless.tensei.ui.components.animateRailItemOffset(
                                        centerOffset = centerOffset,
                                        isScrolling = isCastScrolling,
                                        label = "castCenterOffset",
                                    )

                                    val railTransforms = com.blissless.tensei.ui.components.railScrollTransforms(animatedOffset)
                                    val scrollScale = railTransforms.scale
                                    val scrollAlpha = railTransforms.alpha
                                    val scrollTranslationX = railTransforms.translationX
                                    val scrollRotationY = railTransforms.rotationY

                                    val indexFloat = index.toFloat()
                                    val staggeredProgress = ((cinematicProgress * 1000f - (indexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                    val easedProgress = easeOut(staggeredProgress)

                                    val introScale = if (cinematicProgress >= 1f) 1f else 0.85f + easedProgress * 0.15f
                                    val introAlpha = if (cinematicProgress >= 1f) 1f else easedProgress

                                    Column(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .graphicsLayer {
                                                scaleX = introScale * scrollScale
                                                scaleY = introScale * scrollScale
                                                this.alpha = introAlpha * scrollAlpha
                                                translationX = scrollTranslationX
                                                rotationY = scrollRotationY
                                                cameraDistance = cameraDistancePx
                                            }
                                            .clickable {
                                                val id = character.id
                                                onCharacterClick(id)
                                            }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f)
                                        ) {
                                            Card(
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxSize(),
                                                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                                            ) {
                                                AsyncImage(
                                                    model = character.image?.large,
                                                    contentDescription = character.name?.full,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            character.name?.full ?: stringResource(R.string.anime_detail_unknown),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.height(28.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
}

@Composable
internal fun AnimeDetailStaffItem(
    staffList: List<com.blissless.tensei.data.models.DetailedAnimeStaffEdge>,
    alpha: Float,
    cinematicProgress: Float,
    density: androidx.compose.ui.unit.Density,
    onStaffClick: (Int) -> Unit,
    onViewAllStaff: () -> Unit,
) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
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
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        stringResource(R.string.anime_detail_staff),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        stringResource(R.string.anime_detail_production_crew),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                if (staffList.isNotEmpty()) {
                                    TextButton(onClick = onViewAllStaff) {
                                        Text(stringResource(R.string.anime_detail_view_all), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                                    }
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
                            val cameraDistancePx = with(density) { 12.dp.toPx() }

                            LazyRow(
                                state = staffListState,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                itemsIndexed(
                                    items = staffList,
                                    key = { index, _ -> "staff_$index" }
                                ) { index, staffEdge ->
                                    staffEdge.node?.let { staff ->
                                        val layoutInfo = staffListState.layoutInfo
                                        val visibleItems = layoutInfo.visibleItemsInfo
                                        val itemInfo = visibleItems.find { it.index == index }

                                        val centerOffset = if (itemInfo != null) {
                                            val itemCenter = itemInfo.offset + itemInfo.size / 2
                                            val screenCenter = (layoutInfo.viewportSize.width / 2).toFloat()
                                            (itemCenter - screenCenter) / screenCenter
                                        } else {
                                            0f
                                        }

                                        val animatedOffset = com.blissless.tensei.ui.components.animateRailItemOffset(
                                            centerOffset = centerOffset,
                                            isScrolling = isStaffScrolling,
                                            label = "staffCenterOffset",
                                        )

                                        val railTransforms = com.blissless.tensei.ui.components.railScrollTransforms(animatedOffset)
                                        val scrollScale = railTransforms.scale
                                        val scrollAlpha = railTransforms.alpha
                                        val scrollTranslationX = railTransforms.translationX
                                        val scrollRotationY = railTransforms.rotationY

                                        val indexFloat = index.toFloat()
                                        val staggeredProgress = ((cinematicProgress * 1000f - (indexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                        val easedProgress = easeOut(staggeredProgress)

                                        val introScale = if (cinematicProgress >= 1f) 1f else 0.85f + easedProgress * 0.15f
                                        val introAlpha = if (cinematicProgress >= 1f) 1f else easedProgress

                                        Column(
                                            modifier = Modifier
                                                .width(80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { staffEdge.node.id.let { id -> onStaffClick(id) } }
                                                .graphicsLayer {
                                                    scaleX = introScale * scrollScale
                                                    scaleY = introScale * scrollScale
                                                    this.alpha = introAlpha * scrollAlpha
                                                    translationX = scrollTranslationX
                                                    rotationY = scrollRotationY
                                                    cameraDistance = cameraDistancePx
                                                }
                                                .padding(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(1f)
                                            ) {
                                                Card(
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.fillMaxSize(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A))
                                                ) {
                                                    AsyncImage(
                                                        model = staff.image?.large,
                                                        contentDescription = staff.name?.full,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                staff.name?.full ?: stringResource(R.string.anime_detail_unknown),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.height(28.dp)
                                            )
                                            StaffEdgeJobText(
                                                role = staffEdge.role,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
}


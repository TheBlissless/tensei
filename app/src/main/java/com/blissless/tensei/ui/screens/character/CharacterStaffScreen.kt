package com.blissless.tensei.ui.screens.character

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.blissless.tensei.MainViewModel
import com.blissless.tensei.data.models.CharacterData
import com.blissless.tensei.data.models.StaffData
import com.blissless.tensei.ui.components.TenseiScrimChip
import com.blissless.tensei.ui.components.anilistAnnotated
import com.blissless.tensei.ui.components.rememberCinematicAnimation
import com.blissless.tensei.ui.screens.details.easeOut
import com.blissless.tensei.ui.theme.Spacing
import com.blissless.tensei.ui.theme.ratingColorOnArtwork

/**
 * Two lines of labelSmall (lineHeight 14sp) in dp: the box every rail card title and
 * role reserves. Titles come in one and two lines, and a card that grows changes both
 * the row's height and where the centre of the animated cover sits, so the covers
 * wobble up and down while the row scrolls.
 */
private val RailTextTwoLines = 28.dp

@Composable
fun CharacterScreen(
    characterId: Int,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit = onDismiss,
    onMediaClick: (Int, String?) -> Unit,
    onCharacterClick: ((Int) -> Unit)? = null,
    onStaffClick: ((Int) -> Unit)? = null,
    stackDepth: Int = 1
) {
    var character by remember { mutableStateOf<CharacterData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    LaunchedEffect(characterId) {
        isLoading = true
        character = viewModel.fetchCharacter(characterId)
        isLoading = false
    }

    Dialog(
        onDismissRequest = onNavigateBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            character?.let { char ->
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp + navigationBarsPadding.calculateBottomPadding())
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp + statusBarsPadding.calculateTopPadding())
                        ) {
                            AsyncImage(
                                model = char.image?.large,
                                contentDescription = char.name?.full,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )
                            IconButton(
                                onClick = { if (stackDepth > 2) onDismiss() else onNavigateBack() },
                                modifier = Modifier
                                    .padding(top = statusBarsPadding.calculateTopPadding() + 8.dp, start = 16.dp)
                                    .align(Alignment.TopStart)
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .zIndex(10f)
                            ) {
                                Icon(
                                    if (stackDepth > 2) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (stackDepth > 2) "Close" else "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val shareText = buildString {
                                        char.name?.full?.let { append(it) }
                                        append("\n\n")
                                        append(com.blissless.tensei.network.Endpoints.AniList.characterPageUrl(char.id))
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .padding(top = statusBarsPadding.calculateTopPadding() + 8.dp, end = 16.dp)
                                    .align(Alignment.TopEnd)
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .zIndex(10f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = char.name?.full ?: "Unknown",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            char.name?.native?.let { native ->
                                Text(
                                    text = native,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!char.description.isNullOrEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "About",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val charAnimeTitles = char.anime?.nodes?.flatMap { node ->
                                        listOfNotNull(
                                            node.title?.romaji?.let { it to node.id },
                                            node.title?.english?.let { it to node.id }
                                        )
                                    }?.toMap() ?: emptyMap()
                                    val annotatedBio = anilistAnnotated(
                                        char.description,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        MaterialTheme.colorScheme.primary,
                                        charAnimeTitles
                                    )
                                    @Suppress("DEPRECATION")
                                    ClickableText(
                                        text = annotatedBio,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        onClick = { offset ->
                                            val animeAnnot = annotatedBio.getStringAnnotations("ANIME", offset, offset).firstOrNull()
                                            if (animeAnnot != null) {
                                                onMediaClick(animeAnnot.item.toInt(), null)
                                            } else {
                                                val urlAnnot = annotatedBio.getStringAnnotations("URL", offset, offset).firstOrNull()
                                                if (urlAnnot != null) {
                                                    val url = urlAnnot.item
                                                    val charMatch = Regex("anilist\\.co/character/(\\d+)").find(url)
                                                    val staffMatch = Regex("anilist\\.co/staff/(\\d+)").find(url)
                                                    if (charMatch != null) {
                                                        onCharacterClick?.invoke(charMatch.groupValues[1].toInt())
                                                    } else if (staffMatch != null) {
                                                        onStaffClick?.invoke(staffMatch.groupValues[1].toInt())
                                                    } else {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, url.toUri())
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    char.anime?.nodes?.let { animeList ->
                        if (animeList.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Appears In",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val appearsListState = rememberLazyListState()
                                        val isAppearsScrolling by remember {
                                            derivedStateOf { appearsListState.isScrollInProgress }
                                        }
                                        val appearsCinematic = rememberCinematicAnimation("character_appears_in", isVisible = true, playOncePerSession = true)
                                        val cameraDistancePx = with(LocalDensity.current) { 12.dp.toPx() }
                                        LazyRow(
                                            state = appearsListState,
                                            contentPadding = PaddingValues(start = 0.dp, end = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(animeList) { index, anime ->
                                                val layoutInfo by remember { derivedStateOf { appearsListState.layoutInfo } }
                                                val visibleItems = layoutInfo.visibleItemsInfo
                                                val itemInfo = visibleItems.find { it.index == index }
                                                val centerOffset = if (itemInfo != null) {
                                                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                                                    val screenCenter = (layoutInfo.viewportSize.width / 2).toFloat()
                                                    (itemCenter - screenCenter) / screenCenter
                                                } else 0f
                                                val animatedOffset by animateFloatAsState(
                                                    targetValue = if (isAppearsScrolling) centerOffset.coerceIn(-1.5f, 1.5f) else 0f,
                                                    animationSpec = if (isAppearsScrolling) {
                                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                    } else {
                                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                                    },
                                                    label = "appearsCenterOffset"
                                                )
                                                val scrollScale = 1f - (animatedOffset.absoluteValue * 0.25f).coerceAtMost(0.25f)
                                                val scrollAlpha = 1f - (animatedOffset.absoluteValue * 0.4f).coerceAtMost(0.6f)
                                                val scrollTranslationX = animatedOffset * -20f
                                                val scrollRotationY = (animatedOffset * 15f).coerceIn(-15f, 15f)
                                                val indexFloat = index.toFloat()
                                                val staggeredProgress = ((appearsCinematic * 1000f - (indexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                                val easedProgress = easeOut(staggeredProgress)
                                                val introScale = if (appearsCinematic >= 1f) 1f else 0.85f + easedProgress * 0.15f
                                                val introAlpha = if (appearsCinematic >= 1f) 1f else easedProgress
                                                Column(
                                                    modifier = Modifier
                                                        .width(100.dp)
                                                        .graphicsLayer {
                                                            scaleX = introScale * scrollScale
                                                            scaleY = introScale * scrollScale
                                                            this.alpha = introAlpha * scrollAlpha
                                                            translationX = scrollTranslationX
                                                            rotationY = scrollRotationY
                                                            cameraDistance = cameraDistancePx
                                                        }
                                                        .clickable { onMediaClick(anime.id, anime.format) },
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    Box(modifier = Modifier.aspectRatio(3f / 4f)) {
                                                        Card(
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier.fillMaxSize(),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                            )
                                                        ) {
                                                            AsyncImage(
                                                                model = anime.coverImage?.extraLarge,
                                                                contentDescription = anime.title?.romaji,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        }
                                                        anime.episodes?.takeIf { it > 0 }?.let { epCount ->
                                                            TenseiScrimChip(
                                                                text = if (epCount == 1) "1 ep" else "$epCount eps",
                                                                modifier = Modifier.padding(Spacing.sm).align(Alignment.TopStart)
                                                            )
                                                        }
                                                        anime.averageScore?.takeIf { it > 0 }?.let { score ->
                                                            TenseiScrimChip(
                                                                text = String.format(java.util.Locale.US, "%.1f", score / 10.0),
                                                                color = ratingColorOnArtwork(),
                                                                modifier = Modifier.padding(Spacing.sm).align(Alignment.TopEnd)
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        anime.title?.english ?: anime.title?.romaji ?: "Unknown",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis,
                                                        color = MaterialTheme.colorScheme.onBackground,
                                                        modifier = Modifier.fillMaxWidth().height(RailTextTwoLines)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StaffScreen(
    staffId: Int,
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    onNavigateBack: () -> Unit = onDismiss,
    onMediaClick: (Int, String?) -> Unit,
    onCharacterClick: ((Int) -> Unit)? = null,
    onStaffClick: ((Int) -> Unit)? = null,
    stackDepth: Int = 1
) {
    var staff by remember { mutableStateOf<StaffData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val statusBarsPadding = WindowInsets.statusBars.asPaddingValues()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues()

    LaunchedEffect(staffId) {
        isLoading = true
        loadError = null
        staff = viewModel.fetchStaff(staffId)
        if (staff == null) {
            loadError = "Failed to load staff data"
        }
        isLoading = false
    }

    Dialog(
        onDismissRequest = onNavigateBack,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (staff != null) {
                staff?.let { staffData ->
                    LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp + navigationBarsPadding.calculateBottomPadding())
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp + statusBarsPadding.calculateTopPadding())
                        ) {
                            AsyncImage(
                                model = staffData.image?.large,
                                contentDescription = staffData.name?.full,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        androidx.compose.ui.graphics.Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.background
                                            )
                                        )
                                    )
                            )
                            IconButton(
                                onClick = { if (stackDepth > 2) onDismiss() else onNavigateBack() },
                                modifier = Modifier
                                    .padding(top = statusBarsPadding.calculateTopPadding() + 8.dp, start = 16.dp)
                                    .align(Alignment.TopStart)
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .zIndex(10f)
                            ) {
                                Icon(
                                    if (stackDepth > 2) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = if (stackDepth > 2) "Close" else "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            IconButton(
                                onClick = {
                                    val shareText = buildString {
                                        staffData.name?.full?.let { append(it) }
                                        append("\n\n")
                                        append(com.blissless.tensei.network.Endpoints.AniList.staffPageUrl(staffData.id))
                                    }
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, null)
                                    context.startActivity(shareIntent)
                                },
                                modifier = Modifier
                                    .padding(top = statusBarsPadding.calculateTopPadding() + 8.dp, end = 16.dp)
                                    .align(Alignment.TopEnd)
                                    .size(40.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .zIndex(10f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }

                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = staffData.name?.full ?: "Unknown",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            staffData.name?.native?.let { native ->
                                Text(
                                    text = native,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (!staffData.description.isNullOrEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Work,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "About",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val staffAnimeTitles = staffData.anime?.edges?.mapNotNull { edge ->
                                        edge.node
                                    }?.flatMap { node ->
                                        listOfNotNull(
                                            node.title?.romaji?.let { it to node.id },
                                            node.title?.english?.let { it to node.id }
                                        )
                                    }?.toMap() ?: emptyMap()
                                    val annotatedBio = anilistAnnotated(
                                        staffData.description,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        MaterialTheme.colorScheme.primary,
                                        staffAnimeTitles
                                    )
                                    @Suppress("DEPRECATION")
                                    ClickableText(
                                        text = annotatedBio,
                                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                                        onClick = { offset ->
                                            val animeAnnot = annotatedBio.getStringAnnotations("ANIME", offset, offset).firstOrNull()
                                            if (animeAnnot != null) {
                                                onMediaClick(animeAnnot.item.toInt(), null)
                                            } else {
                                                val urlAnnot = annotatedBio.getStringAnnotations("URL", offset, offset).firstOrNull()
                                                if (urlAnnot != null) {
                                                    val url = urlAnnot.item
                                                    val charMatch = Regex("anilist\\.co/character/(\\d+)").find(url)
                                                    val staffMatch = Regex("anilist\\.co/staff/(\\d+)").find(url)
                                                    if (charMatch != null) {
                                                        onCharacterClick?.invoke(charMatch.groupValues[1].toInt())
                                                    } else if (staffMatch != null) {
                                                        onStaffClick?.invoke(staffMatch.groupValues[1].toInt())
                                                    } else {
                                                        context.startActivity(
                                                            Intent(Intent.ACTION_VIEW, url.toUri())
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    staffData.anime?.edges?.let { edges ->
                        if (edges.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            "Worked On",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        val workedListState = rememberLazyListState()
                                        val isWorkedScrolling by remember {
                                            derivedStateOf { workedListState.isScrollInProgress }
                                        }
                                        val workedCinematic = rememberCinematicAnimation("staff_worked_on", isVisible = true, playOncePerSession = true)
                                        val workedCameraDistancePx = with(LocalDensity.current) { 12.dp.toPx() }
                                        LazyRow(
                                            state = workedListState,
                                            contentPadding = PaddingValues(start = 0.dp, end = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            itemsIndexed(edges) { index, edge ->
                                                val layoutInfo by remember { derivedStateOf { workedListState.layoutInfo } }
                                                val visibleItems = layoutInfo.visibleItemsInfo
                                                val itemInfo = visibleItems.find { it.index == index }
                                                val centerOffset = if (itemInfo != null) {
                                                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                                                    val screenCenter = (layoutInfo.viewportSize.width / 2).toFloat()
                                                    (itemCenter - screenCenter) / screenCenter
                                                } else 0f
                                                val animatedOffset by animateFloatAsState(
                                                    targetValue = if (isWorkedScrolling) centerOffset.coerceIn(-1.5f, 1.5f) else 0f,
                                                    animationSpec = if (isWorkedScrolling) {
                                                        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                                    } else {
                                                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                                                    },
                                                    label = "workedCenterOffset"
                                                )
                                                val scrollScale = 1f - (animatedOffset.absoluteValue * 0.25f).coerceAtMost(0.25f)
                                                val scrollAlpha = 1f - (animatedOffset.absoluteValue * 0.4f).coerceAtMost(0.6f)
                                                val scrollTranslationX = animatedOffset * -20f
                                                val scrollRotationY = (animatedOffset * 15f).coerceIn(-15f, 15f)
                                                val indexFloat = index.toFloat()
                                                val staggeredProgress = ((workedCinematic * 1000f - (indexFloat * 40f)) / 1000f).coerceIn(0f, 1f)
                                                val easedProgress = easeOut(staggeredProgress)
                                                val introScale = if (workedCinematic >= 1f) 1f else 0.85f + easedProgress * 0.15f
                                                val introAlpha = if (workedCinematic >= 1f) 1f else easedProgress
                                                val anime = edge.node
                                                val role = edge.staffRole
                                                if (anime != null) {
                                                    Column(
                                                        modifier = Modifier
                                                            .width(110.dp)
                                                            .graphicsLayer {
                                                                scaleX = introScale * scrollScale
                                                                scaleY = introScale * scrollScale
                                                                this.alpha = introAlpha * scrollAlpha
                                                                translationX = scrollTranslationX
                                                                rotationY = scrollRotationY
                                                                cameraDistance = workedCameraDistancePx
                                                            }
                                                            .clickable { onMediaClick(anime.id, anime.format) },
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Box(modifier = Modifier.aspectRatio(3f / 4f)) {
                                                            Card(
                                                                shape = RoundedCornerShape(12.dp),
                                                                modifier = Modifier.fillMaxSize(),
                                                                colors = CardDefaults.cardColors(
                                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                                )
                                                            ) {
                                                                AsyncImage(
                                                                    model = anime.coverImage?.extraLarge,
                                                                    contentDescription = anime.title?.romaji,
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier.fillMaxSize()
                                                                )
                                                            }
                                                            anime.episodes?.takeIf { it > 0 }?.let { epCount ->
                                                                TenseiScrimChip(
                                                                    text = if (epCount == 1) "1 ep" else "$epCount eps",
                                                                    modifier = Modifier.padding(Spacing.sm).align(Alignment.TopStart)
                                                                )
                                                            }
                                                            anime.averageScore?.takeIf { it > 0 }?.let { score ->
                                                                TenseiScrimChip(
                                                                    text = String.format(java.util.Locale.US, "%.1f", score / 10.0),
                                                                    color = ratingColorOnArtwork(),
                                                                    modifier = Modifier.padding(Spacing.sm).align(Alignment.TopEnd)
                                                                )
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text(
                                                            anime.title?.english ?: anime.title?.romaji ?: "Unknown",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onBackground,
                                                            modifier = Modifier.fillMaxWidth().height(RailTextTwoLines)
                                                        )
                                                        // Always laid out, even with no role: a missing role must not
                                                        // make one card shorter than its neighbours.
                                                        Text(
                                                            text = role.orEmpty(),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.fillMaxWidth().height(RailTextTwoLines)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                    }
                }
            } else {
                 Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = loadError ?: "Could not load staff data",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


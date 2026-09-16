package eu.kanade.tachiyomi.animesource.model

/**
 * Seekbar thumbnail-preview metadata returned by
 * `AnimeHttpSource.getVideoThumbnails(video)`.
 *
 * Ported from Aniyomi's `ThumbnailInfo` at
 * `source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/animesource/model/ThumbnailInfo.kt`.
 *
 * The player renders preview thumbnails on the seekbar by:
 *   1. Downloading the thumbnail sprite image (a single image
 *      containing N x M frames arranged in a grid).
 *   2. For each video position, looking up the right tile via
 *      `TileInfo.startUs <= position < nextTile.startUs`.
 *   3. Cropping the sprite and rendering it above the seekbar.
 *
 * @param url the sprite image URL.
 * @param tiles the per-tile metadata (one entry per seekbar tick).
 * @param intervalUs the spacing between tiles in microseconds (0 if
 *   the tiles have varying intervals).
 */
data class ThumbnailInfo(
    val url: String,
    val tiles: List<TileInfo> = emptyList(),
    val intervalUs: Long = 0L,
)

/**
 * One tile in a thumbnail sprite grid.
 *
 * @param startUs the video position (in microseconds) at which this
 *   tile is the active preview.
 * @param x the x-coordinate of the tile's top-left corner inside the
 *   sprite image, in pixels.
 * @param y the y-coordinate of the tile's top-left corner inside the
 *   sprite image, in pixels.
 * @param width the tile width, in pixels.
 * @param height the tile height, in pixels.
 */
data class TileInfo(
    val startUs: Long,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

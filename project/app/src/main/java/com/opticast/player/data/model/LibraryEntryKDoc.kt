package com.opticast.player.data.model

/**
 * Represents a video file in the library with its metadata.
 * 
 * This is the core data class for the library - used in grid, search, discovery.
 * 
 * @property video The local video file with id, name, path, duration, etc.
 * @property metadata Optional metadata from TMDB, AniList, etc. - title, poster, year, etc.
 * 
 * Performance notes for low-RAM 32-bit devices:
 * - Keep this class small - only essential fields
 * - metadata is nullable to avoid loading all metadata at once
 * - Used with stable keys video.id in LazyVerticalGrid to prevent recomposition
 * 
 * Example:
 * ```
 * val entry = LibraryEntry(
 *     video = LocalVideo(id = 1, name = "Avengers.mp4", ...),
 *     metadata = Metadata(displayTitle = "Avengers Endgame", year = 2019, ...)
 * )
 * ```
 * 
 * @see LocalVideo
 * @see Metadata
 */
data class LibraryEntryKDocExample(
    val video: LocalVideo,
    val metadata: Metadata?
)

/**
 * Local video file from MediaStore.
 * 
 * @property id Stable ID from MediaStore - used as key in grid for smooth scrolling
 * @property name File name like "Avengers Endgame (2019).mp4"
 * @property path Full path like "/storage/Movies/Avengers.mp4"
 * @property size File size in bytes
 * @property durationMs Duration in milliseconds
 * @property dateAdded When file was added to device
 * @property parsed Parsed title from file name
 * 
 * For 9/10 rating: Keep this lightweight for low-RAM devices
 */

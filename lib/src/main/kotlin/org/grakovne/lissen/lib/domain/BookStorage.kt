package org.grakovne.lissen.lib.domain

import androidx.annotation.Keep
import java.io.Serializable

/**
 * Categorizes how an audiobook is physically stored on the server.
 */
@Keep
enum class BookStorageType {
    /**
     * The entire book is a single physical file (e.g., M4B).
     */
    MONOLITH,

    /**
     * The book is split into multiple files, each containing multiple chapters.
     */
    SEGMENTED,

    /**
     * Each file corresponds to exactly one chapter.
     */
    ATOMIC
}

/**
 * Represents a physical storage unit (file) as a semantic "Volume" or "Part".
 */
@Keep
data class BookVolume(
    val id: String,
    val name: String,
    val size: Long,
    val chapters: List<PlayingChapter>,
    val isDownloaded: Boolean
) : Serializable

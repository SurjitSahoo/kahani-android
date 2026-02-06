package org.grakovne.lissen.lib.domain

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import java.io.Serializable

@Keep
data class DetailedItem(
  val id: String = "",
  val title: String = "",
  val subtitle: String? = null,
  val author: String? = null,
  val narrator: String? = null,
  val publisher: String? = null,
  val series: List<BookSeries> = emptyList(),
  val year: String? = null,
  val abstract: String? = null,
  val files: List<BookFile> = emptyList(),
  val chapters: List<PlayingChapter> = emptyList(),
  val progress: MediaProgress? = null,
  val libraryId: String? = null,
  val libraryType: LibraryType? = null,
  val localProvided: Boolean = false,
  val createdAt: Long = 0,
  val updatedAt: Long = 0,
) : Serializable

@Keep
data class BookFile(
  val id: String = "",
  val name: String = "",
  val duration: Double = 0.0,
  val mimeType: String = "",
  val size: Long = 0,
) : Serializable

@Keep
data class MediaProgress(
  val currentTime: Double = 0.0,
  val isFinished: Boolean = false,
  val lastUpdate: Long = 0,
) : Serializable

@Keep
data class PlayingChapter(
  val available: Boolean = false,
  val podcastEpisodeState: BookChapterState? = null,
  val duration: Double = 0.0,
  val start: Double = 0.0,
  val end: Double = 0.0,
  val title: String = "",
  val id: String = "",
) : Serializable

@Keep
data class BookSeries(
  val serialNumber: String? = null,
  val name: String = "",
) : Serializable

@Keep
enum class BookChapterState {
  FINISHED,
}

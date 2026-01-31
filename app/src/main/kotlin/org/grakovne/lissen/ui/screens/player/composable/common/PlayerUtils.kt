package org.grakovne.lissen.ui.screens.player.composable.common

import android.content.Context
import org.grakovne.lissen.R
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.LibraryType

fun sanitizeChapterTitle(
  title: String?,
  bookTitle: String?,
): String? {
  if (title.isNullOrBlank()) return null

  var sanitized = title

  // Strip common file extensions
  val extensions = listOf(".mp3", ".m4b", ".m4a", ".aac", ".flac", ".wav")
  extensions.forEach { ext ->
    if (sanitized!!.endsWith(ext, ignoreCase = true)) {
      sanitized = sanitized!!.substring(0, sanitized!!.length - ext.length)
    }
  }

  // Remove book title if it's a prefix or suffix (ignore case and spacing)
  bookTitle?.let { bt ->
    val cleanBt = bt.replace(Regex("[^A-Za-z0-9]"), "").lowercase()
    val cleanTitle = sanitized!!.replace(Regex("[^A-Za-z0-9]"), "").lowercase()

    if (cleanTitle.startsWith(cleanBt)) {
      // Find the character index in 'sanitized' that corresponds to the end of cleanBt
      var charCount = 0
      var lastMatchIndex = -1

      for (i in sanitized!!.indices) {
        if (sanitized!![i].isLetterOrDigit()) {
          charCount++
        }
        if (charCount == cleanBt.length) {
          lastMatchIndex = i
          break
        }
      }

      if (lastMatchIndex != -1) {
        // The actual cut-off point is lastMatchIndex + 1, plus any following non-alphanumeric separators
        var finalCutOff = lastMatchIndex + 1
        while (finalCutOff < sanitized!!.length && !sanitized!![finalCutOff].isLetterOrDigit()) {
          finalCutOff++
        }
        sanitized = sanitized!!.substring(finalCutOff)
      }
    }
  }

  // Clean up underscores and dashes
  sanitized = sanitized!!.replace('_', ' ').replace('-', ' ')

  // Final trim and return
  return sanitized!!.trim().capitalize()
}

fun String.capitalize() = this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

fun provideChapterIndexTitle(
  currentTrackIndex: Int,
  book: DetailedItem?,
  libraryType: LibraryType,
  context: Context,
): String =
  when (libraryType) {
    LibraryType.LIBRARY ->
      context.getString(
        R.string.player_screen_now_playing_title_chapter_of,
        currentTrackIndex + 1,
        book?.chapters?.size ?: "?",
      )

    LibraryType.PODCAST ->
      context.getString(
        R.string.player_screen_now_playing_title_podcast_of,
        currentTrackIndex + 1,
        book?.chapters?.size ?: "?",
      )

    LibraryType.UNKNOWN ->
      context.getString(
        R.string.player_screen_now_playing_title_item_of,
        currentTrackIndex + 1,
        book?.chapters?.size ?: "?",
      )
  }

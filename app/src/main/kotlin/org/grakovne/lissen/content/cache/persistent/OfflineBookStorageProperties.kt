package org.grakovne.lissen.content.cache.persistent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineBookStorageProperties
  @Inject
  constructor(
    @ApplicationContext private val context: Context,
  ) {
    private fun baseFolder(): File =
      context
        .getExternalFilesDir(MEDIA_CACHE_FOLDER)
        ?.takeIf { it.exists() || it.mkdirs() && it.canWrite() }
        ?: context
          .cacheDir
          .resolve(MEDIA_CACHE_FOLDER)
          .apply {
            if (exists().not()) {
              mkdirs()
            }
          }

    fun provideBookCache(bookId: String): File = baseFolder().resolve(bookId)

    fun provideMediaCachePath(
      bookId: String,
      fileId: String,
    ): File =
      baseFolder()
        .resolve(bookId)
        .resolve(fileId)

    fun provideBookCoverPath(bookId: String): File = provideBookCoverThumbPath(bookId)

    fun provideBookCoverRawPath(bookId: String): File =
      baseFolder()
        .resolve(bookId)
        .resolve("cover_raw.img")

    fun provideBookCoverThumbPath(bookId: String): File =
      baseFolder()
        .resolve(bookId)
        .resolve("cover_thumb.img")

    companion object {
      const val MEDIA_CACHE_FOLDER = "media_cache"
    }
  }

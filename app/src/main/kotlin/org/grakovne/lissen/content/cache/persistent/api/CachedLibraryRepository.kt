package org.grakovne.lissen.content.cache.persistent.api

import org.grakovne.lissen.content.cache.persistent.converter.CachedLibraryEntityConverter
import org.grakovne.lissen.content.cache.persistent.dao.CachedLibraryDao
import org.grakovne.lissen.lib.domain.Library
import org.grakovne.lissen.persistence.preferences.LissenSharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CachedLibraryRepository
  @Inject
  constructor(
    private val dao: CachedLibraryDao,
    private val converter: CachedLibraryEntityConverter,
    private val preferences: LissenSharedPreferences,
  ) {
    suspend fun cacheLibraries(libraries: List<Library>) {
      val host = preferences.getHost() ?: ""
      val username = preferences.getUsername() ?: ""

      dao.updateLibraries(libraries, host, username)
    }

    suspend fun fetchLibraries(): List<Library> {
      val host = preferences.getHost() ?: ""
      val username = preferences.getUsername() ?: ""

      return dao
        .fetchLibraries(host, username)
        .map { converter.apply(it) }
    }
  }

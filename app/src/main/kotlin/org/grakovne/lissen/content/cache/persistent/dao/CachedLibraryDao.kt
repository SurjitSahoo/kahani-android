package org.grakovne.lissen.content.cache.persistent.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.grakovne.lissen.content.cache.persistent.entity.CachedLibraryEntity
import org.grakovne.lissen.lib.domain.Library

@Dao
interface CachedLibraryDao {
  @Transaction
  suspend fun updateLibraries(
    libraries: List<Library>,
    host: String?,
    username: String?,
  ) {
    val entities =
      libraries.map {
        CachedLibraryEntity(
          id = it.id,
          title = it.title,
          type = it.type,
          host = host,
          username = username,
        )
      }

    upsertLibraries(entities)
    deleteLibrariesExcept(entities.map { it.id }, host, username)
  }

  @Transaction
  @Query("SELECT * FROM libraries WHERE id = :libraryId")
  suspend fun fetchLibrary(libraryId: String): CachedLibraryEntity?

  @Transaction
  @Query(
    "SELECT * FROM libraries WHERE ((:host IS NULL AND host IS NULL) OR (host = :host)) AND ((:username IS NULL AND username IS NULL) OR (username = :username))",
  )
  suspend fun fetchLibraries(
    host: String?,
    username: String?,
  ): List<CachedLibraryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertLibraries(libraries: List<CachedLibraryEntity>)

  @Query(
    "DELETE FROM libraries WHERE id NOT IN (:ids) AND ((:host IS NULL AND host IS NULL) OR (host = :host)) AND ((:username IS NULL AND username IS NULL) OR (username = :username))",
  )
  suspend fun deleteLibrariesExcept(
    ids: List<String>,
    host: String?,
    username: String?,
  )
}

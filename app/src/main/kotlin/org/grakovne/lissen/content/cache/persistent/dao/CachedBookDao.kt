package org.grakovne.lissen.content.cache.persistent.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.flow.Flow
import org.grakovne.lissen.common.moshi
import org.grakovne.lissen.content.cache.persistent.entity.BookChapterEntity
import org.grakovne.lissen.content.cache.persistent.entity.BookEntity
import org.grakovne.lissen.content.cache.persistent.entity.BookFileEntity
import org.grakovne.lissen.content.cache.persistent.entity.BookSeriesDto
import org.grakovne.lissen.content.cache.persistent.entity.CachedBookEntity
import org.grakovne.lissen.content.cache.persistent.entity.MediaProgressEntity
import org.grakovne.lissen.lib.domain.DetailedItem
import org.grakovne.lissen.lib.domain.PlayingChapter

@Dao
interface CachedBookDao {
  @Transaction
  suspend fun upsertCachedBook(
    book: DetailedItem,
    host: String,
    username: String,
    fetchedChapters: List<PlayingChapter>,
    droppedChapters: List<PlayingChapter>,
  ) {
    val bookEntity =
      BookEntity(
        id = book.id,
        title = book.title,
        subtitle = book.subtitle,
        author = book.author,
        narrator = book.narrator,
        duration = book.chapters.sumOf { it.duration }.toInt(),
        libraryId = book.libraryId,
        libraryType = book.libraryType,
        year = book.year,
        abstract = book.abstract,
        publisher = book.publisher,
        createdAt = book.createdAt,
        updatedAt = book.updatedAt,
        host = host,
        username = username,
        seriesNames =
          book
            .series
            .joinToString(" ") { it.name },
        seriesJson =
          book
            .series
            .map { BookSeriesDto(title = it.name, sequence = it.serialNumber) }
            .let {
              adapter.toJson(it)
            },
      )

    val bookFiles =
      book
        .files
        .map { file ->
          BookFileEntity(
            bookFileId = file.id,
            name = file.name,
            duration = file.duration,
            mimeType = file.mimeType,
            bookId = book.id,
          )
        }

    val cachedBookChapters =
      fetchCachedBook(book.id)
        ?.chapters
        ?: emptyList()

    val bookChapters =
      book
        .chapters
        .map { chapter ->
          val fetched = fetchedChapters.any { it.id == chapter.id }
          val exists = cachedBookChapters.any { it.bookChapterId == chapter.id && it.isCached }
          val dropped = droppedChapters.any { it.id == chapter.id }

          val cached =
            when (dropped) {
              true -> false
              false -> fetched || exists
            }

          BookChapterEntity(
            bookChapterId = chapter.id,
            duration = chapter.duration,
            start = chapter.start,
            end = chapter.end,
            title = chapter.title,
            bookId = book.id,
            isCached = cached,
          )
        }

    val mediaProgress =
      book
        .progress
        ?.let { progress ->
          MediaProgressEntity(
            bookId = book.id,
            currentTime = progress.currentTime,
            isFinished = progress.isFinished,
            lastUpdate = progress.lastUpdate,
            host = host,
            username = username,
          )
        }

    upsertBook(bookEntity)
    upsertBookFiles(bookFiles)
    upsertBookChapters(bookChapters)
    mediaProgress?.let { upsertMediaProgress(it) }
  }

  @Transaction
  @RawQuery
  suspend fun fetchCachedBooks(query: SupportSQLiteQuery): List<BookEntity>

  @Query(
    """
    SELECT COUNT(*) FROM detailed_books
    WHERE ((:libraryId IS NULL AND libraryId IS NULL) OR (libraryId = :libraryId))
      AND host = :host
      AND username = :username
    """,
  )
  suspend fun countCachedBooks(
    libraryId: String?,
    host: String,
    username: String,
  ): Int

  @Transaction
  @RawQuery
  suspend fun searchBooks(query: SupportSQLiteQuery): List<BookEntity>

  @Transaction
  @RawQuery(observedEntities = [BookEntity::class, MediaProgressEntity::class])
  fun fetchRecentlyListenedCachedBooksFlow(query: SupportSQLiteQuery): Flow<List<CachedBookEntity>>

  @Transaction
  @RawQuery
  suspend fun fetchRecentlyListenedCachedBooks(query: SupportSQLiteQuery): List<BookEntity>

  @Transaction
  @Query("SELECT * FROM detailed_books WHERE id = :bookId")
  suspend fun fetchCachedBook(bookId: String): CachedBookEntity?

  @Transaction
  @Query("SELECT * FROM detailed_books WHERE id = :bookId")
  fun fetchBookFlow(bookId: String): Flow<CachedBookEntity?>

  @Query("SELECT COUNT(*) > 0 FROM detailed_books WHERE id = :bookId")
  fun isBookCached(bookId: String): LiveData<Boolean>

  @Transaction
  @Query(
    """
    SELECT * FROM detailed_books
    WHERE EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1)
    ORDER BY title ASC, libraryId ASC
    LIMIT :pageSize
    OFFSET (:pageNumber * :pageSize)
    """,
  )
  suspend fun fetchCachedItems(
    pageSize: Int,
    pageNumber: Int,
  ): List<CachedBookEntity>

  @Query(
    """
    SELECT COUNT(*) FROM detailed_books 
    WHERE EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1)
    """,
  )
  suspend fun fetchCachedItemsCount(): Int

  @Query(
    """
    SELECT COUNT(*) > 0
    FROM book_chapters
    WHERE bookId       = :bookId
      AND bookChapterId = :chapterId
      AND isCached      = 1
    """,
  )
  fun isBookChapterCached(
    bookId: String,
    chapterId: String,
  ): LiveData<Boolean>

  @Query(
    """
    SELECT COUNT(*) > 0
    FROM book_chapters
    WHERE bookId = :bookId
      AND isCached = 1
    """,
  )
  fun hasDownloadedChapters(bookId: String): LiveData<Boolean>

  @Query(
    """
        SELECT MAX(mp.lastUpdate)
        FROM detailed_books AS d
        INNER JOIN media_progress AS mp ON d.id = mp.bookId
        WHERE (d.libraryId IS NULL OR d.libraryId = :libraryId)
        """,
  )
  suspend fun fetchLatestUpdate(libraryId: String): Long?

  @Transaction
  @Query("SELECT * FROM detailed_books WHERE id = :bookId")
  suspend fun fetchBook(bookId: String): BookEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBook(book: BookEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBooks(books: List<BookEntity>)

  @Query("SELECT * FROM detailed_books WHERE id IN (:bookIds)")
  suspend fun fetchBooks(bookIds: List<String>): List<BookEntity>

  @Update
  suspend fun updateBook(book: BookEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBookFiles(files: List<BookFileEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBookChapters(chapters: List<BookChapterEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertMediaProgress(progress: MediaProgressEntity)

  @Transaction
  @Query("SELECT * FROM media_progress WHERE bookId = :bookId")
  suspend fun fetchMediaProgress(bookId: String): MediaProgressEntity?

  @Delete
  suspend fun deleteBook(book: BookEntity)

  @Transaction
  @Query(
    """
    DELETE FROM detailed_books
    WHERE id NOT IN (SELECT DISTINCT bookId FROM book_chapters WHERE isCached = 1)
      AND (host != :currentHost OR username != :currentUsername)
    """,
  )
  suspend fun deleteNonDownloadedBooks(
    currentHost: String,
    currentUsername: String,
  )

  companion object {
    val type = Types.newParameterizedType(List::class.java, BookSeriesDto::class.java)
    val adapter = moshi.adapter<List<BookSeriesDto>>(type)
  }
}

package org.grakovne.lissen.content.cache.persistent.api

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

class RecentRequestBuilder {
  private var libraryId: String? = null
  private var downloadedOnly: Boolean = false
  private var limit: Int = 10
  private var host: String? = null
  private var username: String? = null

  fun libraryId(id: String?) = apply { this.libraryId = id }

  fun downloadedOnly(enabled: Boolean) = apply { this.downloadedOnly = enabled }

  fun limit(limit: Int) = apply { this.limit = limit }

  fun host(host: String?) = apply { this.host = host }

  fun username(username: String?) = apply { this.username = username }

  fun build(): SupportSQLiteQuery {
    val args = mutableListOf<Any>()

    val libraryClause =
      when (val libraryId = libraryId) {
        null -> "detailed_books.libraryId IS NULL"
        else -> {
          args.add(libraryId)
          "(detailed_books.libraryId = ? OR detailed_books.libraryId IS NULL)"
        }
      }

    val isolationClause =
      when (downloadedOnly) {
        true -> "EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1)"
        false -> {
          val host = host
          val username = username

          if (!host.isNullOrEmpty() && !username.isNullOrEmpty()) {
            args.add(host)
            args.add(username)
            "((detailed_books.host = ? AND detailed_books.username = ?) OR EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1))"
          } else {
            "EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1)"
          }
        }
      }

    val sql =
      """
      SELECT DISTINCT detailed_books.* FROM detailed_books
      INNER JOIN media_progress ON detailed_books.id = media_progress.bookId
      WHERE $libraryClause 
      AND $isolationClause
      AND media_progress.currentTime > 1.0
      AND media_progress.isFinished = 0
      ORDER BY media_progress.lastUpdate DESC
      LIMIT $limit
      """.trimIndent()

    return SimpleSQLiteQuery(sql, args.toTypedArray())
  }
}

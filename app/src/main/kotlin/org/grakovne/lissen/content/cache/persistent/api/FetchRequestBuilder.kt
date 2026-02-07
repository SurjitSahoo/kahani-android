package org.grakovne.lissen.content.cache.persistent.api

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

class FetchRequestBuilder {
  private var libraryId: String? = null
  private var pageNumber: Int = 0
  private var pageSize: Int = 20
  private var orderField: String = "title"
  private var orderDirection: String = "ASC"
  private var downloadedOnly: Boolean = false
  private var host: String? = null
  private var username: String? = null

  fun libraryId(id: String?) = apply { this.libraryId = id }

  fun pageNumber(number: Int) = apply { this.pageNumber = number }

  fun pageSize(size: Int) = apply { this.pageSize = size }

  fun orderField(field: String) = apply { this.orderField = field }

  fun orderDirection(direction: String) = apply { this.orderDirection = direction }

  fun downloadedOnly(enabled: Boolean) = apply { this.downloadedOnly = enabled }

  fun host(host: String?) = apply { this.host = host }

  fun username(username: String?) = apply { this.username = username }

  fun build(): SupportSQLiteQuery {
    val args = mutableListOf<Any>()

    val libraryClause =
      when (val libraryId = libraryId) {
        null -> "libraryId IS NULL"
        else -> {
          args.add(libraryId)
          "(libraryId = ? OR libraryId IS NULL)"
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
            "((host = ? AND username = ?) OR EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1))"
          } else {
            "EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1)"
          }
        }
      }

    val field =
      when (orderField) {
        "title", "author", "duration" -> "detailed_books.$orderField"
        else -> "detailed_books.title"
      }

    val direction =
      when (orderDirection.uppercase()) {
        "ASC", "DESC" -> orderDirection.uppercase()
        else -> "ASC"
      }

    args.add(pageSize)
    args.add(pageNumber * pageSize)

    val sql =
      """
      SELECT detailed_books.* FROM detailed_books
      WHERE $libraryClause AND $isolationClause
      ORDER BY $field $direction, detailed_books.id ASC
      LIMIT ? OFFSET ?
      """.trimIndent()

    return SimpleSQLiteQuery(sql, args.toTypedArray())
  }
}

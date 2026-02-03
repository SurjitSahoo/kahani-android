package org.grakovne.lissen.content.cache.persistent.api

import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery

class SearchRequestBuilder {
  private var libraryId: String? = null
  private var searchQuery: String = ""
  private var orderField: String = "title"
  private var orderDirection: String = "ASC"
  private var host: String = ""
  private var username: String = ""

  fun libraryId(id: String?) = apply { this.libraryId = id }

  fun searchQuery(query: String) = apply { this.searchQuery = query }

  fun orderField(field: String) = apply { this.orderField = field }

  fun orderDirection(direction: String) = apply { this.orderDirection = direction }

  fun host(host: String) = apply { this.host = host }

  fun username(username: String) = apply { this.username = username }

  fun build(): SupportSQLiteQuery {
    val args = mutableListOf<Any>()

    val libraryClause =
      when (val libraryId = libraryId) {
        null -> "(libraryId IS NULL)"
        else -> {
          args.add(libraryId)
          "(libraryId = ? OR libraryId IS NULL)"
        }
      }

    val searchClause = "(title LIKE ? OR author LIKE ? OR seriesNames LIKE ?)"
    val pattern = "%$searchQuery%"
    args.add(pattern)
    args.add(pattern)
    args.add(pattern)

    val isolationClause =
      "((host = ? AND username = ?) OR EXISTS (SELECT 1 FROM book_chapters WHERE bookId = detailed_books.id AND isCached = 1))"
    args.add(host)
    args.add(username)

    val field =
      when (orderField) {
        "title", "author", "duration" -> orderField
        else -> "title"
      }

    val direction =
      when (orderDirection.uppercase()) {
        "ASC", "DESC" -> orderDirection.uppercase()
        else -> "ASC"
      }

    val sql =
      """
      SELECT * FROM detailed_books
      WHERE $libraryClause AND $searchClause AND $isolationClause
      ORDER BY $field $direction
      """.trimIndent()

    return SimpleSQLiteQuery(sql, args.toTypedArray())
  }
}

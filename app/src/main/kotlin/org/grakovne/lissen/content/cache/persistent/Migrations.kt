package org.grakovne.lissen.content.cache.persistent

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.grakovne.lissen.lib.domain.LibraryType

val MIGRATION_1_2 =
  object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE detailed_books RENAME TO detailed_books_old")

      db.execSQL(
        """
        CREATE TABLE detailed_books (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            author TEXT,
            duration INTEGER NOT NULL
        )
        """.trimIndent(),
      )

      db.execSQL(
        """
        INSERT INTO detailed_books (id, title, author, duration)
        SELECT id, title, author, duration FROM detailed_books_old
        """.trimIndent(),
      )

      db.execSQL("DROP TABLE detailed_books_old")
    }
  }

val MIGRATION_2_3 =
  object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE libraries ADD COLUMN type TEXT")

      db.execSQL(
        """
        UPDATE libraries
        SET type = '${LibraryType.LIBRARY.name}'
        """.trimIndent(),
      )

      db.execSQL(
        """
        CREATE TABLE libraries_new (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            type TEXT NOT NULL
        )
        """.trimIndent(),
      )

      db.execSQL(
        """
        INSERT INTO libraries_new (id, title, type)
        SELECT id, title, type FROM libraries
        """.trimIndent(),
      )

      db.execSQL("DROP TABLE libraries")
      db.execSQL("ALTER TABLE libraries_new RENAME TO libraries")
    }
  }

val MIGRATION_3_4 =
  object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN libraryId TEXT")

      db.execSQL(
        """
        UPDATE detailed_books
        SET libraryId = NULL
        """.trimIndent(),
      )
    }
  }

val MIGRATION_4_5 =
  object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE book_chapters ADD COLUMN isCached INTEGER NOT NULL DEFAULT 0")

      db.execSQL(
        """
        UPDATE book_chapters
        SET isCached = 1
        """.trimIndent(),
      )
    }
  }

val MIGRATION_5_6 =
  object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN year TEXT")
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN abstract TEXT")
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN publisher TEXT")
    }
  }

val MIGRATION_6_7 =
  object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN subtitle TEXT")
    }
  }

val MIGRATION_7_8 =
  object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        """
        CREATE TABLE book_series (
            id TEXT NOT NULL PRIMARY KEY,
            bookId TEXT NOT NULL,
            serialNumber INTEGER NOT NULL,
            name TEXT NOT NULL,
            FOREIGN KEY (bookId) REFERENCES detailed_books(id) ON DELETE CASCADE
        )
        """.trimIndent(),
      )

      db.execSQL("CREATE INDEX index_book_series_bookId ON book_series(bookId)")
    }
  }

val MIGRATION_8_9 =
  object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL(
        """
        CREATE TABLE book_series_new (
            id TEXT NOT NULL PRIMARY KEY,
            bookId TEXT NOT NULL,
            serialNumber TEXT,
            name TEXT NOT NULL,
            FOREIGN KEY (bookId) REFERENCES detailed_books(id) ON DELETE CASCADE
        )
        """.trimIndent(),
      )

      db.execSQL(
        """
        INSERT INTO book_series_new (id, bookId, serialNumber, name)
        SELECT id, bookId, serialNumber, name FROM book_series
        """.trimIndent(),
      )

      db.execSQL("DROP TABLE book_series")
      db.execSQL("ALTER TABLE book_series_new RENAME TO book_series")
      db.execSQL("CREATE INDEX index_book_series_bookId ON book_series(bookId)")
    }
  }

val MIGRATION_9_10 =
  object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("DROP TABLE IF EXISTS book_series")
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN seriesJson TEXT")
    }
  }

val MIGRATION_10_11 =
  object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
      val now = System.currentTimeMillis() / 1000

      database.execSQL(
        """
        CREATE TABLE detailed_books_new (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            author TEXT,
            duration INTEGER NOT NULL,
            abstract TEXT,
            subtitle TEXT,
            year TEXT,
            libraryId TEXT,
            publisher TEXT,
            seriesJson TEXT,
            createdAt INTEGER NOT NULL
        )
        """.trimIndent(),
      )

      database.execSQL(
        """
        INSERT INTO detailed_books_new (
            id, title, author, duration, abstract, subtitle, year, libraryId, publisher, seriesJson, createdAt
        )
        SELECT 
            id, title, author, duration, abstract, subtitle, year, libraryId, publisher, seriesJson, $now
        FROM detailed_books
        """.trimIndent(),
      )

      database.execSQL("DROP TABLE detailed_books")
      database.execSQL("ALTER TABLE detailed_books_new RENAME TO detailed_books")
    }
  }

val MIGRATION_11_12 =
  object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
      val now = System.currentTimeMillis() / 1000

      database.execSQL(
        """
        CREATE TABLE detailed_books_new (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            author TEXT,
            duration INTEGER NOT NULL,
            abstract TEXT,
            subtitle TEXT,
            year TEXT,
            libraryId TEXT,
            publisher TEXT,
            seriesJson TEXT,
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
        )
        """.trimIndent(),
      )

      database.execSQL(
        """
        INSERT INTO detailed_books_new (
            id, title, author, duration, abstract, subtitle, year, libraryId, publisher, seriesJson, createdAt, updatedAt
        )
        SELECT 
            id, title, author, duration, abstract, subtitle, year, libraryId, publisher, seriesJson, createdAt, $now
        FROM detailed_books
        """.trimIndent(),
      )

      database.execSQL("DROP TABLE detailed_books")
      database.execSQL("ALTER TABLE detailed_books_new RENAME TO detailed_books")
    }
  }

val MIGRATION_12_13 =
  object : Migration(12, 13) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN narrator TEXT")
    }
  }

val MIGRATION_13_14 =
  object : Migration(13, 14) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN seriesNames TEXT")
    }
  }

val MIGRATION_14_15 =
  object : Migration(14, 15) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE detailed_books ADD COLUMN libraryType TEXT")
    }
  }

fun produceMigration15_16(
  host: String?,
  username: String?,
) = object : Migration(15, 16) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // We add columns as nullable without a fixed DEFAULT value.
    // If the user is logged in during migration, we can tag existing data.
    // If they are logged out, existing data remains NULL (owned by "legacy/unknown").
    val hostClause = if (host != null) " DEFAULT '$host'" else ""
    val usernameClause = if (username != null) " DEFAULT '$username'" else ""

    db.execSQL("ALTER TABLE detailed_books ADD COLUMN host TEXT$hostClause")
    db.execSQL("ALTER TABLE detailed_books ADD COLUMN username TEXT$usernameClause")

    db.execSQL("ALTER TABLE media_progress ADD COLUMN host TEXT$hostClause")
    db.execSQL("ALTER TABLE media_progress ADD COLUMN username TEXT$usernameClause")

    db.execSQL("ALTER TABLE libraries ADD COLUMN host TEXT$hostClause")
    db.execSQL("ALTER TABLE libraries ADD COLUMN username TEXT$usernameClause")
  }
}

val MIGRATION_16_17 =
  object : Migration(16, 17) {
    override fun migrate(db: SupportSQLiteDatabase) {
      db.execSQL("ALTER TABLE book_files ADD COLUMN size INTEGER NOT NULL DEFAULT 0")
    }
  }

val MIGRATION_17_18 =
  object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
      // Recreate detailed_books to ensure host and username are nullable and correctly positioned
      db.execSQL("ALTER TABLE detailed_books RENAME TO detailed_books_old")
      db.execSQL(
        """
        CREATE TABLE detailed_books (
            id TEXT NOT NULL PRIMARY KEY, 
            title TEXT NOT NULL, 
            subtitle TEXT, 
            author TEXT, 
            narrator TEXT, 
            year TEXT, 
            abstract TEXT, 
            publisher TEXT, 
            duration INTEGER NOT NULL, 
            libraryId TEXT, 
            libraryType TEXT, 
            seriesJson TEXT, 
            seriesNames TEXT, 
            createdAt INTEGER NOT NULL, 
            updatedAt INTEGER NOT NULL, 
            host TEXT, 
            username TEXT
        )
        """.trimIndent(),
      )
      db.execSQL(
        """
        INSERT INTO detailed_books (
            id, title, subtitle, author, narrator, year, abstract, publisher, 
            duration, libraryId, libraryType, seriesJson, seriesNames, createdAt, updatedAt, host, username
        )
        SELECT 
            id, title, subtitle, author, narrator, year, abstract, publisher, 
            duration, libraryId, libraryType, seriesJson, seriesNames, createdAt, updatedAt, host, username 
        FROM detailed_books_old
        """.trimIndent(),
      )
      db.execSQL("DROP TABLE detailed_books_old")

      // Recreate libraries
      db.execSQL("ALTER TABLE libraries RENAME TO libraries_old")
      db.execSQL(
        """
        CREATE TABLE libraries (
            id TEXT NOT NULL PRIMARY KEY, 
            title TEXT NOT NULL, 
            type TEXT NOT NULL, 
            host TEXT, 
            username TEXT
        )
        """.trimIndent(),
      )
      db.execSQL(
        """
        INSERT INTO libraries (id, title, type, host, username)
        SELECT id, title, type, host, username FROM libraries_old
        """.trimIndent(),
      )
      db.execSQL("DROP TABLE libraries_old")

      // Recreate media_progress
      db.execSQL("ALTER TABLE media_progress RENAME TO media_progress_old")
      db.execSQL(
        """
        CREATE TABLE media_progress (
            bookId TEXT NOT NULL PRIMARY KEY, 
            currentTime REAL NOT NULL, 
            isFinished INTEGER NOT NULL, 
            lastUpdate INTEGER NOT NULL, 
            host TEXT, 
            username TEXT,
            FOREIGN KEY(bookId) REFERENCES detailed_books(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
      )
      db.execSQL(
        """
        INSERT INTO media_progress (bookId, currentTime, isFinished, lastUpdate, host, username)
        SELECT bookId, currentTime, isFinished, lastUpdate, host, username FROM media_progress_old
        """.trimIndent(),
      )
      db.execSQL("DROP TABLE media_progress_old")
      db.execSQL("CREATE INDEX IF NOT EXISTS index_media_progress_bookId ON media_progress (bookId)")

      // Recreate book_files to move size to the end
      db.execSQL("ALTER TABLE book_files RENAME TO book_files_old")
      db.execSQL(
        """
        CREATE TABLE book_files (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 
            bookFileId TEXT NOT NULL, 
            name TEXT NOT NULL, 
            duration REAL NOT NULL, 
            mimeType TEXT NOT NULL, 
            bookId TEXT NOT NULL, 
            size INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(bookId) REFERENCES detailed_books(id) ON UPDATE NO ACTION ON DELETE CASCADE
        )
        """.trimIndent(),
      )
      db.execSQL(
        """
        INSERT INTO book_files (id, bookFileId, name, duration, mimeType, bookId, size)
        SELECT id, bookFileId, name, duration, mimeType, bookId, size FROM book_files_old
        """.trimIndent(),
      )
      db.execSQL("DROP TABLE book_files_old")
      db.execSQL("CREATE INDEX IF NOT EXISTS index_book_files_bookId ON book_files (bookId)")
    }
  }

val MIGRATION_18_19 =
  object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
      // Schema realignment to fix identity hash mismatch
    }
  }

package com.copyplay.data.browser

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.copyplay.domain.browser.CachedFolderListing
import com.copyplay.domain.browser.CopypartyPath
import com.copyplay.domain.browser.FolderEntry
import com.copyplay.domain.browser.FolderListing
import com.copyplay.domain.browser.FolderListingCache
import com.copyplay.domain.browser.SubtitleCandidate
import com.copyplay.domain.server.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SqliteFolderListingCache(
    context: Context,
) : FolderListingCache {
    private val helper = FolderCacheOpenHelper(context.applicationContext)

    override suspend fun get(server: ServerConfig, path: CopypartyPath): CachedFolderListing? =
        withContext(Dispatchers.IO) {
            helper.readableDatabase.query(
                FolderCacheOpenHelper.TableName,
                arrayOf("payload_json", "fetched_at_epoch_millis"),
                "server_base_url = ? AND folder_path = ?",
                arrayOf(server.baseUrl, path.encodedRelativePath()),
                null,
                null,
                null,
                "1",
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@withContext null
                val payload = JsonFormat.decodeFromString<CachedFolderPayload>(
                    cursor.getString(cursor.getColumnIndexOrThrow("payload_json")),
                )
                val fetchedAt = cursor.getLong(cursor.getColumnIndexOrThrow("fetched_at_epoch_millis"))
                CachedFolderListing(
                    listing = payload.toDomain(server),
                    fetchedAtEpochMillis = fetchedAt,
                )
            }
        }

    override suspend fun put(listing: FolderListing, fetchedAtEpochMillis: Long) {
        withContext(Dispatchers.IO) {
            val payload = CachedFolderPayload.fromDomain(listing)
            val values = ContentValues().apply {
                put("server_base_url", listing.server.baseUrl)
                put("folder_path", listing.path.encodedRelativePath())
                put("payload_json", JsonFormat.encodeToString(payload))
                put("fetched_at_epoch_millis", fetchedAtEpochMillis)
            }
            helper.writableDatabase.insertWithOnConflict(
                FolderCacheOpenHelper.TableName,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
    }

    private companion object {
        val JsonFormat = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

private class FolderCacheOpenHelper(
    context: Context,
) : SQLiteOpenHelper(context, DatabaseName, null, DatabaseVersion) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TableName (
                server_base_url TEXT NOT NULL,
                folder_path TEXT NOT NULL,
                payload_json TEXT NOT NULL,
                fetched_at_epoch_millis INTEGER NOT NULL,
                PRIMARY KEY(server_base_url, folder_path)
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TableName")
        onCreate(db)
    }

    companion object {
        const val DatabaseName = "copyplay_folder_cache.db"
        const val DatabaseVersion = 1
        const val TableName = "folder_listing_cache"
    }
}

@Serializable
private data class CachedFolderPayload(
    val pathSegments: List<String>,
    val visibleEntries: List<CachedVisibleEntry>,
    val hiddenSubtitles: List<CachedSubtitleCandidate>,
) {
    fun toDomain(server: ServerConfig): FolderListing =
        FolderListing(
            server = server,
            path = CopypartyPath(pathSegments),
            visibleEntries = visibleEntries.map { it.toDomain() },
            hiddenSubtitles = hiddenSubtitles.map { it.toDomain() },
        )

    companion object {
        fun fromDomain(listing: FolderListing): CachedFolderPayload =
            CachedFolderPayload(
                pathSegments = listing.path.segments,
                visibleEntries = listing.visibleEntries.map(CachedVisibleEntry::fromDomain),
                hiddenSubtitles = listing.hiddenSubtitles.map(CachedSubtitleCandidate::fromDomain),
            )
    }
}

@Serializable
private data class CachedVisibleEntry(
    val type: String,
    val name: String,
    val pathSegments: List<String>,
    val sizeBytes: Long? = null,
    val modifiedEpochSeconds: Long? = null,
) {
    fun toDomain(): FolderEntry =
        when (type) {
            DirectoryType -> FolderEntry.Directory(
                name = name,
                path = CopypartyPath(pathSegments),
            )

            VideoType -> FolderEntry.Video(
                name = name,
                path = CopypartyPath(pathSegments),
                sizeBytes = sizeBytes,
                modifiedEpochSeconds = modifiedEpochSeconds,
            )

            else -> error("Unknown cached folder entry type: $type")
        }

    companion object {
        const val DirectoryType = "directory"
        const val VideoType = "video"

        fun fromDomain(entry: FolderEntry): CachedVisibleEntry =
            when (entry) {
                is FolderEntry.Directory -> CachedVisibleEntry(
                    type = DirectoryType,
                    name = entry.name,
                    pathSegments = entry.path.segments,
                )

                is FolderEntry.Video -> CachedVisibleEntry(
                    type = VideoType,
                    name = entry.name,
                    pathSegments = entry.path.segments,
                    sizeBytes = entry.sizeBytes,
                    modifiedEpochSeconds = entry.modifiedEpochSeconds,
                )
            }
    }
}

@Serializable
private data class CachedSubtitleCandidate(
    val name: String,
    val pathSegments: List<String>,
    val sizeBytes: Long? = null,
    val modifiedEpochSeconds: Long? = null,
) {
    fun toDomain(): SubtitleCandidate =
        SubtitleCandidate(
            name = name,
            path = CopypartyPath(pathSegments),
            sizeBytes = sizeBytes,
            modifiedEpochSeconds = modifiedEpochSeconds,
        )

    companion object {
        fun fromDomain(candidate: SubtitleCandidate): CachedSubtitleCandidate =
            CachedSubtitleCandidate(
                name = candidate.name,
                pathSegments = candidate.path.segments,
                sizeBytes = candidate.sizeBytes,
                modifiedEpochSeconds = candidate.modifiedEpochSeconds,
            )
    }
}

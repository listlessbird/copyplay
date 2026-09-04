package com.copyplay.data.playback

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.copyplay.domain.playback.PlaybackIdentity
import com.copyplay.domain.playback.PlaybackPreferences
import com.copyplay.domain.playback.PlaybackPreferencesStore
import com.copyplay.domain.playback.PlaybackProgress
import com.copyplay.domain.playback.PlaybackProgressStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.playbackDataStore by preferencesDataStore(name = "playback_state")

class DataStorePlaybackStore(
    context: Context,
) : PlaybackProgressStore, PlaybackPreferencesStore {
    private val dataStore = context.playbackDataStore

    override val preferences: Flow<PlaybackPreferences> = dataStore.data.map { preferences ->
        PlaybackPreferences(
            autoplayNext = preferences[AutoplayNextKey] ?: true,
        )
    }

    override val progressEntries: Flow<List<PlaybackProgress>> = dataStore.data.map { preferences ->
        decodeProgress(preferences[ProgressEntriesKey]).entries
    }

    override suspend fun setAutoplayNext(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AutoplayNextKey] = enabled
        }
    }

    override suspend fun get(identity: PlaybackIdentity): PlaybackProgress? =
        progressEntries.first().firstOrNull { it.identity == identity }

    override suspend fun save(progress: PlaybackProgress) {
        dataStore.edit { preferences ->
            val existing = decodeProgress(preferences[ProgressEntriesKey]).entries
            val updated = (existing.filterNot { it.identity == progress.identity } + progress)
                .sortedByDescending { it.updatedAtEpochMillis }
                .take(MaxProgressEntries)
            preferences[ProgressEntriesKey] = Json.encodeToString(StoredPlaybackProgressEntries(updated))
        }
    }

    private fun decodeProgress(raw: String?): StoredPlaybackProgressEntries =
        raw
            ?.let { runCatching { Json.decodeFromString<StoredPlaybackProgressEntries>(it) }.getOrNull() }
            ?: StoredPlaybackProgressEntries(emptyList())

    private companion object {
        const val MaxProgressEntries = 100
        val AutoplayNextKey = booleanPreferencesKey("autoplay_next")
        val ProgressEntriesKey = stringPreferencesKey("progress_entries")
    }
}

@Serializable
private data class StoredPlaybackProgressEntries(
    val entries: List<PlaybackProgress>,
)

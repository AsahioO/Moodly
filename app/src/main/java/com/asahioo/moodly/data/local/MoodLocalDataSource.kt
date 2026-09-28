package com.asahioo.moodly.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.asahioo.moodly.data.model.AppData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

private val Context.moodlyDataStore: DataStore<Preferences> by preferencesDataStore(name = "moodly")

/**
 * Persistencia local. Guarda [AppData] como JSON en DataStore: escrituras atómicas,
 * lecturas reactivas y sin bloquear el hilo principal.
 */
class MoodLocalDataSource(context: Context, private val json: Json) {

    private val store = context.applicationContext.moodlyDataStore

    /** En el primer arranque (o si el documento no se puede leer) emite un estado vacío. */
    val data: Flow<AppData> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[KEY]?.let(::decode) ?: AppData() }
        .flowOn(Dispatchers.Default)

    suspend fun update(transform: (AppData) -> AppData) {
        store.edit { prefs ->
            val current = prefs[KEY]?.let(::decode) ?: AppData()
            prefs[KEY] = encode(transform(current))
        }
    }

    private fun decode(raw: String): AppData? = try {
        json.decodeFromString(AppData.serializer(), raw)
    } catch (e: SerializationException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }

    private fun encode(data: AppData): String = json.encodeToString(AppData.serializer(), data)

    private companion object {
        val KEY = stringPreferencesKey("app_state_v1")
    }
}

package com.mks.hackerspaces

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val Context.dataStore by preferencesDataStore(name = "settings")

class SpaceRepository(private val context: Context) {
    private val FAVORITES_KEY = stringPreferencesKey("favorites")
    private val NOTIFICATION_SPACES_KEY = stringPreferencesKey("notification_spaces")
    
    // Settings Keys
    private val REFRESH_INTERVAL_KEY = longPreferencesKey("refresh_interval") // in minutes
    private val NOTIFICATIONS_ENABLED_KEY = booleanPreferencesKey("notifications_enabled")
    private val UNSAFE_SSL_KEY = booleanPreferencesKey("unsafe_ssl")
    private val MAX_SPACES_KEY = intPreferencesKey("max_spaces")

    private val gson = Gson()
    
    // In-memory cache for spaces
    private val spacesCache = ConcurrentHashMap<String, SpaceApi>()
    private val CACHE_DURATION_MS = 15 * 60 * 1000L // 15 minutes
    private val spacesCacheTimestamp = ConcurrentHashMap<String, Long>()

    // Settings Flows
    val refreshInterval: Flow<Long> = context.dataStore.data.map { it[REFRESH_INTERVAL_KEY] ?: 15L }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED_KEY] ?: false }
    val unsafeSsl: Flow<Boolean> = context.dataStore.data.map { it[UNSAFE_SSL_KEY] ?: true }
    val maxSpaces: Flow<Int> = context.dataStore.data.map { it[MAX_SPACES_KEY] ?: 200 }

    suspend fun updateRefreshInterval(minutes: Long) {
        context.dataStore.edit { it[REFRESH_INTERVAL_KEY] = minutes }
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED_KEY] = enabled }
    }

    suspend fun updateUnsafeSsl(enabled: Boolean) {
        context.dataStore.edit { it[UNSAFE_SSL_KEY] = enabled }
        // We need to invalidate the Retrofit instance effectively
        RetrofitInstance.reset()
    }

    suspend fun updateMaxSpaces(max: Int) {
        context.dataStore.edit { it[MAX_SPACES_KEY] = max }
    }

    suspend fun getDirectory(): Map<String, String> {
        // We pass the SSL setting check to the network layer indirectly via RetrofitInstance re-creation
        // But here we just call the api.
        return RetrofitInstance.getApi(isUnsafeSslEnabled()).getDirectory()
    }

    private suspend fun isUnsafeSslEnabled(): Boolean {
        return try {
             context.dataStore.data.first()[UNSAFE_SSL_KEY] ?: true
        } catch (e: Exception) { true }
    }

    suspend fun getSpace(url: String, forceRefresh: Boolean = false): SpaceApi {
        if (!forceRefresh) {
            val cachedSpace = spacesCache[url]
            val timestamp = spacesCacheTimestamp[url]
            
            if (cachedSpace != null && timestamp != null) {
                if (System.currentTimeMillis() - timestamp < CACHE_DURATION_MS) {
                    return cachedSpace
                }
            }
        }
        
        return withContext(Dispatchers.IO) {
            val isUnsafe = context.dataStore.data.first()[UNSAFE_SSL_KEY] ?: true
            val space = RetrofitInstance.getApi(isUnsafe).getSpace(url).copy(url = url)
            
            if (space.space == null) throw Exception("Invalid space data: name missing")
            
            spacesCache[url] = space
            spacesCacheTimestamp[url] = System.currentTimeMillis()
            space
        }
    }

    val favorites: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[FAVORITES_KEY] ?: "[]"
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type)
        }
        
    val notificationSpaces: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[NOTIFICATION_SPACES_KEY] ?: "[]"
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type)
        }

    suspend fun toggleFavorite(spaceUrl: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[FAVORITES_KEY] ?: "[]"
            val type = object : TypeToken<MutableSet<String>>() {}.type
            val currentFavorites: MutableSet<String> = gson.fromJson(json, type)
            
            if (currentFavorites.contains(spaceUrl)) {
                currentFavorites.remove(spaceUrl)
            } else {
                currentFavorites.add(spaceUrl)
            }
            
            preferences[FAVORITES_KEY] = gson.toJson(currentFavorites)
        }
    }
    
    suspend fun toggleNotification(spaceUrl: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[NOTIFICATION_SPACES_KEY] ?: "[]"
            val type = object : TypeToken<MutableSet<String>>() {}.type
            val currentNotifications: MutableSet<String> = gson.fromJson(json, type)
            
            if (currentNotifications.contains(spaceUrl)) {
                currentNotifications.remove(spaceUrl)
            } else {
                currentNotifications.add(spaceUrl)
            }
            
            preferences[NOTIFICATION_SPACES_KEY] = gson.toJson(currentNotifications)
        }
    }
}

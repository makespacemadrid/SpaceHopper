package com.mks.hackerspaces

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class MapFilter {
    MY_SPACES,
    ALL_SPACES
}

class SpaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SpaceRepository(application)

    private val _directory = MutableStateFlow<Map<String, String>>(emptyMap())
    val directory: StateFlow<Map<String, String>> = _directory.asStateFlow()

    private val _mySpaces = MutableStateFlow<List<SpaceApi>>(emptyList())
    val mySpaces: StateFlow<List<SpaceApi>> = _mySpaces.asStateFlow()
    
    private val _mapFilter = MutableStateFlow(MapFilter.MY_SPACES)
    val mapFilter: StateFlow<MapFilter> = _mapFilter.asStateFlow()

    // Cache for all loaded spaces (from directory)
    private val _allLoadedSpaces = MutableStateFlow<Map<String, SpaceApi>>(emptyMap())

    // Combined flow for map spaces based on filter
    val spacesWithLocation: StateFlow<List<SpaceApi>> = combine(
        _mapFilter,
        _mySpaces,
        _allLoadedSpaces
    ) { filter, mySpaces, allSpacesMap ->
        when (filter) {
            MapFilter.MY_SPACES -> mySpaces
            MapFilter.ALL_SPACES -> allSpacesMap.values.toList()
        }
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSpace = MutableStateFlow<SpaceApi?>(null)
    val selectedSpace: StateFlow<SpaceApi?> = _selectedSpace.asStateFlow()
    
    val refreshInterval = repository.refreshInterval
    val notificationsEnabled = repository.notificationsEnabled
    val unsafeSsl = repository.unsafeSsl
    val maxSpaces = repository.maxSpaces

    private var loadAllJob: Job? = null

    init {
        fetchDirectory()
        viewModelScope.launch {
            repository.favorites.collect { favorites ->
                refreshMySpaces(favorites)
            }
        }
        
        // React to unsafe SSL changes to maybe refresh data? 
        // For now, next fetch will pick it up.
    }
    
    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateIn(
        scope: kotlinx.coroutines.CoroutineScope,
        started: kotlinx.coroutines.flow.SharingStarted,
        initialValue: T
    ): StateFlow<T> {
        val mutable = MutableStateFlow(initialValue)
        scope.launch {
            this@stateIn.collect {
                mutable.value = it
            }
        }
        return mutable.asStateFlow()
    }

    fun updateRefreshInterval(minutes: Long) {
        viewModelScope.launch { repository.updateRefreshInterval(minutes) }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateNotificationsEnabled(enabled) }
    }

    fun updateUnsafeSsl(enabled: Boolean) {
        viewModelScope.launch { repository.updateUnsafeSsl(enabled) }
    }

    fun updateMaxSpaces(max: Int) {
        viewModelScope.launch { repository.updateMaxSpaces(max) }
    }

    fun setMapFilter(filter: MapFilter) {
        _mapFilter.value = filter
        if (filter == MapFilter.ALL_SPACES && _allLoadedSpaces.value.isEmpty()) {
            loadAllSpaces()
        }
    }

    private fun loadAllSpaces() {
        if (loadAllJob?.isActive == true) return
        
        loadAllJob = viewModelScope.launch {
            val dir = _directory.value
            if (dir.isEmpty()) return@launch
            
            // Get max spaces from settings
            val limit = maxSpaces.first()
            val limitedDir = dir.entries.take(limit)
            
            // Fetch in batches
            val chunkSize = 10
            val chunks = limitedDir.chunked(chunkSize)
            
            for (chunk in chunks) {
                if (_mapFilter.value != MapFilter.ALL_SPACES) break 
                
                val deferred = chunk.map { (_, url) ->
                    async {
                        try {
                            repository.getSpace(url)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
                
                val results = deferred.awaitAll().filterNotNull()
                
                val currentMap = _allLoadedSpaces.value.toMutableMap()
                results.forEach { space ->
                    if (space.location?.lat != null && space.location.lon != null) {
                        currentMap[space.url] = space
                    }
                }
                _allLoadedSpaces.value = currentMap
            }
        }
    }

    fun fetchDirectory() {
        viewModelScope.launch {
            try {
                _directory.value = repository.getDirectory()
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error
            }
        }
    }

    fun refreshMySpaces() {
        viewModelScope.launch {
            val favorites = repository.favorites.first()
            refreshMySpaces(favorites)
        }
    }

    private suspend fun refreshMySpaces(favorites: Set<String>) {
        val deferredSpaces = favorites.map { url ->
            viewModelScope.async {
                try {
                    repository.getSpace(url)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
        }
        val spaces = deferredSpaces.awaitAll().filterNotNull()
        _mySpaces.value = spaces
    }

    fun toggleFavorite(spaceUrl: String) {
        viewModelScope.launch {
            repository.toggleFavorite(spaceUrl)
        }
    }

    fun selectSpace(space: SpaceApi) {
        _selectedSpace.value = space
    }
    
    fun clearSelectedSpace() {
        _selectedSpace.value = null
    }
    
    val favorites = repository.favorites
}

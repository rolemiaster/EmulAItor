package com.swordfish.lemuroid.app.mobile.feature.catalog

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import com.swordfish.lemuroid.app.mobile.feature.catalog.RomSource
import com.swordfish.lemuroid.app.mobile.feature.catalog.SmbCredentials

class CatalogViewModel(
    private val context: Context,
    private val gameMetadataProvider: com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider,
    val romDownloader: RomDownloader // V8.3: Injected Singleton
) : ViewModel() {
    
    companion object {
        private const val TAG = "CatalogViewModel"
        private const val PAGE_SIZE = 50
    }
    
    private val archiveClient = ArchiveOrgClient()
    private val itchClient = ItchIoClient()
    private val itchKeyManager = ItchIoKeyManager(context)
    // val romDownloader = RomDownloader(context, gameMetadataProvider) // Removed manual instantiation
    private val sourceManager = SourceManager(context)
    // V8.4: smbClient removed - now internal to RomDownloader
    
    private val _uiState = MutableStateFlow(UiState(isLoading = true))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val SEARCH_DEBOUNCE_MS = 500L
    
    val downloads = romDownloader.downloads
    
    init {
        Log.d(TAG, "CatalogViewModel init - starting search")
        // Initialize SMB Client and Sources
        // smbClient is already a class member
        val sources = sourceManager.getSources()
        
        // Configure RomDownloader with Library Destination (from Prefs)
        val prefs = SharedPreferencesHelper.getSharedPreferences(context)
        val libType = prefs.getString(SharedPreferencesHelper.KEY_LIBRARY_TYPE, null)
        
        Log.e("ANTIGRAVITY", ">>> INIT CATALOG VIEW MODEL <<<")
        Log.e("ANTIGRAVITY", "Library Type: '$libType'")
        
        val libraryDestination = if (libType == "smb") {
            val server = prefs.getString(SharedPreferencesHelper.KEY_SMB_LIBRARY_SERVER, "") ?: ""
            val share = prefs.getString(SharedPreferencesHelper.KEY_SMB_LIBRARY_SHARE, "") ?: ""
            val path = prefs.getString(SharedPreferencesHelper.KEY_SMB_LIBRARY_PATH, "") ?: ""
            val username = prefs.getString(SharedPreferencesHelper.KEY_SMB_LIBRARY_USERNAME, "") ?: ""
            val password = prefs.getString(SharedPreferencesHelper.KEY_SMB_LIBRARY_PASSWORD, "") ?: ""
            
            Log.e("ANTIGRAVITY", "SMB Config Read -> Server: '$server', Share: '$share', Path: '$path'")
            
            if (server.isNotBlank() && share.isNotBlank()) {
                val creds = if (username.isNotBlank()) SmbCredentials(username, password) else null
                
                // CRITICAL FIX: RomDownloader expects a full SMB URI in the 'path' field to parse server/share correctly.
                // Format: smb://server/share/subpath
                // CRITICAL FIX: RomDownloader expects the path to be clean because RomSource.smb adds the protocol.
                // We just pass the share and path.
                val sharePath = "/$share$path"
                
                RomSource.smb(
                    name = "Library Destination",
                    server = server,
                    path = sharePath, 
                    credentials = creds
                )
            } else {
                Log.e("ANTIGRAVITY", "SMB Config INVALID (Server or Share blank)")
                null
            }
        } else {
            Log.e("ANTIGRAVITY", "Library Type NOT SMB. Skipping.")
            null
        }
        
        Log.e("ANTIGRAVITY", "Final Library Destination: ${libraryDestination?.path}")
        
        // V8.4: SmbClient is now internal to RomDownloader
        romDownloader.setLibraryDestination(libraryDestination)

        // Initialize itch.io API key state from persistent storage
        _uiState.value = _uiState.value.copy(
            itchApiKeyConfigured = itchKeyManager.hasApiKey()
        )

        // Reactively mark itch.io games as fully downloaded once their upload completes.
        // Download IDs follow the pattern "itch_<gameId>_<uploadId>".
        viewModelScope.launch {
            romDownloader.downloads.collect { map ->
                val completedItchGameIds = map.values
                    .filter { it.status == RomDownloader.DownloadStatus.COMPLETED }
                    .mapNotNull { info ->
                        val id = info.id
                        if (id.startsWith("itch_")) {
                            // Format: itch_<gameGuid>_<uploadId>
                            id.removePrefix("itch_").substringBeforeLast("_").ifBlank { null }
                        } else null
                    }
                    .toSet()
                if (completedItchGameIds.isNotEmpty()) {
                    val current = _uiState.value.fullyDownloadedPackIds
                    if (!current.containsAll(completedItchGameIds)) {
                        _uiState.value = _uiState.value.copy(
                            fullyDownloadedPackIds = current + completedItchGameIds
                        )
                    }
                }
            }
        }

        // Initialize search
        searchPacks()
    }
    
    val availableSystems = listOf(
        "snes" to "SNES",
        "nes" to "NES",
        "gba" to "GBA",
        "gbc" to "GB/GBC",
        "genesis" to "Genesis/MD",
        "n64" to "N64",
        "psx" to "PSX",
        "psp" to "PSP",
        "arcade" to "Arcade"
    )
    
    val availableRegions = listOf(
        "", "USA", "EUR", "JPN", "ESP", "FRA", "GER", "ITA", "BRA", "KOR", "CHN", "AUS"
    )
    
    val sortOptions = listOf(
        SortOption.DOWNLOADS, SortOption.NAME, SortOption.SIZE
    )
    
    enum class SortOption { DOWNLOADS, NAME, SIZE }
    
    fun selectSystem(systemId: String) {
        Log.d(TAG, "selectSystem: $systemId")
        _uiState.value = _uiState.value.copy(
            selectedSystem = systemId, 
            packs = emptyList(),
            filteredPacks = emptyList(),
            itchGames = emptyList(),
            currentPage = 1,
            hasMorePages = false
        )
        if (_uiState.value.selectedSource == SourceType.ITCH_IO) {
            searchItchGames()
        } else {
            searchPacks()
        }
    }
    
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        // Debounced auto-search: wait 500ms then search automatically
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchPacks()
        }
    }
    
    fun setRegionFilter(region: String) {
        _uiState.value = _uiState.value.copy(selectedRegion = region)
        applyFiltersAndSort()
    }
    
    fun setSortOption(option: SortOption) {
        _uiState.value = _uiState.value.copy(sortOption = option)
        applyFiltersAndSort()
    }
    
    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var filtered = state.packs
        
        if (state.selectedRegion.isNotEmpty()) {
            filtered = filtered.filter { it.region == state.selectedRegion }
        }
        
        filtered = when (state.sortOption) {
            SortOption.DOWNLOADS -> filtered.sortedByDescending { it.downloads }
            SortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            SortOption.SIZE -> filtered.sortedByDescending { it.sizeBytes }
        }
        
        _uiState.value = state.copy(filteredPacks = filtered)
    }
    
    fun searchPacks() {
        Log.d(TAG, "searchPacks called")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, currentPage = 1)
            
            try {
                val result = archiveClient.searchPacks(
                    systemId = _uiState.value.selectedSystem,
                    query = _uiState.value.searchQuery,
                    page = 1,
                    pageSize = PAGE_SIZE
                )
                Log.d(TAG, "Search completed: ${result.packs.size} packs, total: ${result.totalResults}")
                _uiState.value = _uiState.value.copy(
                    packs = result.packs,
                    filteredPacks = result.packs,
                    totalResults = result.totalResults,
                    currentPage = 1,
                    hasMorePages = result.hasMore,
                    isLoading = false
                )
                applyFiltersAndSort()
            } catch (e: Exception) {
                Log.e(TAG, "Search error", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error desconocido"
                )
            }
        }
    }
    
    fun loadMorePacks() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMorePages) return
        
        val nextPage = _uiState.value.currentPage + 1
        Log.d(TAG, "loadMorePacks: page $nextPage")
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            
            try {
                val result = archiveClient.searchPacks(
                    systemId = _uiState.value.selectedSystem,
                    query = _uiState.value.searchQuery,
                    page = nextPage,
                    pageSize = PAGE_SIZE
                )
                Log.d(TAG, "Loaded more: ${result.packs.size} packs")
                val allPacks = _uiState.value.packs + result.packs
                _uiState.value = _uiState.value.copy(
                    packs = allPacks,
                    currentPage = nextPage,
                    hasMorePages = result.hasMore,
                    isLoadingMore = false
                )
                applyFiltersAndSort()
            } catch (e: Exception) {
                Log.e(TAG, "Load more error", e)
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }
    
    fun onPackSelected(pack: ArchiveOrgClient.RomPack) {
        _uiState.value = _uiState.value.copy(
            selectedPack = pack, 
            downloadableFiles = emptyList(),
            isLoadingFiles = true
        )
        loadDownloadableFiles(pack)
    }
    
    private fun loadDownloadableFiles(pack: ArchiveOrgClient.RomPack) {
        viewModelScope.launch {
            try {
                val files = archiveClient.getItemFiles(pack.archiveIdentifier)
                Log.d(TAG, "Found ${files.size} downloadable files for ${pack.name}")
                
                // V8.6 ANR FIX: Pre-check file existence on background thread
                val downloadedSet = romDownloader.checkBatchFilesExistence(
                    pack.systemId, 
                    files.map { it.name }
                )

                // Mark pack as fully downloaded when every file is present.
                val isFullyDownloaded = files.isNotEmpty() && downloadedSet.size == files.size
                val newFullSet = _uiState.value.fullyDownloadedPackIds.toMutableSet().apply {
                    if (isFullyDownloaded) add(pack.archiveIdentifier) else remove(pack.archiveIdentifier)
                }

                _uiState.value = _uiState.value.copy(
                    downloadableFiles = files,
                    downloadedFiles = downloadedSet,
                    isLoadingFiles = false,
                    fullyDownloadedPackIds = newFullSet
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading files", e)
                _uiState.value = _uiState.value.copy(isLoadingFiles = false)
            }
        }
    }
    
    fun isFileDownloaded(pack: ArchiveOrgClient.RomPack, fileName: String): Boolean {
        // V8.6 ANR FIX: content check via State (O(1)) instead of Disk I/O
        return _uiState.value.downloadedFiles.contains(fileName)
    }
    
    fun startDownload(pack: ArchiveOrgClient.RomPack, file: ArchiveOrgClient.DownloadableFile) {
        Log.d(TAG, "Starting download: ${file.name}")
        try {
            romDownloader.startDownload(pack, file) // Removed user-dependent scope
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start download", e)
             _uiState.value = _uiState.value.copy(
                error = if (e.message == "NO_ROM_PATH_CONFIGURED") "NO_ROM_PATH_CONFIGURED" else e.message
            )
        }
    }
    
    fun downloadAllFiles(pack: ArchiveOrgClient.RomPack, files: List<ArchiveOrgClient.DownloadableFile>) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                // Filtrar archivos que ya están descargados
                val filesToDownload = files.filter { !isFileDownloaded(pack, it.name) }
                Log.d(TAG, "Downloading ${filesToDownload.size} files (${files.size - filesToDownload.size} already downloaded) from ${pack.name}")
                
                // Verificar espacio disponible antes de iniciar descargas (I/O bloqueante)
                val storageCheck = romDownloader.checkStorageForDownload(filesToDownload)
                if (!storageCheck.hasEnoughSpace) {
                    Log.w(TAG, "Not enough storage space: required=${storageCheck.requiredFormatted}, available=${storageCheck.availableFormatted}")
                    _uiState.value = _uiState.value.copy(storageCheckResult = storageCheck)
                    return@launch
                }
                
                filesToDownload.forEach { file ->
                    romDownloader.startDownload(pack, file)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start batch download", e)
                _uiState.value = _uiState.value.copy(
                    error = if (e.message == "NO_ROM_PATH_CONFIGURED") "NO_ROM_PATH_CONFIGURED" else e.message
                )
            }
        }
    }
    
    fun clearStorageError() {
        _uiState.value = _uiState.value.copy(storageCheckResult = null)
    }
    
    fun cancelDownload(downloadId: String) {
        romDownloader.cancelDownload(downloadId)
    }
    
    fun clearCompletedDownloads() {
        romDownloader.clearCompletedDownloads()
    }
    
    fun getActiveDownloadsCount(): Int = romDownloader.getActiveDownloadsCount()
    
    fun clearSelectedPack() {
        _uiState.value = _uiState.value.copy(selectedPack = null, downloadableFiles = emptyList())
    }
    
    // --- itch.io integration ---
    
    fun selectSource(sourceType: SourceType) {
        _uiState.value = _uiState.value.copy(
            selectedSource = sourceType,
            packs = emptyList(),
            filteredPacks = emptyList(),
            itchGames = emptyList(),
            currentPage = 1,
            hasMorePages = false
        )
        if (sourceType == SourceType.ITCH_IO) {
            searchItchGames()
        } else {
            searchPacks()
        }
    }
    
    fun refreshItchApiKeyState() {
        _uiState.value = _uiState.value.copy(
            itchApiKeyConfigured = itchKeyManager.hasApiKey()
        )
    }
    
    /**
     * Trigger itch.io search only if needed (no results cached, not currently loading).
     * Used for background auto-loading when "All" chip is active.
     */
    fun searchItchGamesIfNeeded() {
        val state = _uiState.value
        if (state.itchGames.isNotEmpty()) return
        if (ItchSystemTagMap.isUnsupported(state.selectedSystem)) {
            _uiState.value = state.copy(itchUnsupportedSystem = true)
            return
        }
        searchItchGames()
    }

    fun searchItchGames() {
        val systemId = _uiState.value.selectedSystem

        // Check if system is supported on itch.io
        if (ItchSystemTagMap.isUnsupported(systemId)) {
            _uiState.value = _uiState.value.copy(
                itchGames = emptyList(),
                itchUnsupportedSystem = true
            )
            return
        }
        _uiState.value = _uiState.value.copy(itchUnsupportedSystem = false)

        val tag = ItchSystemTagMap.getTag(systemId)
        if (tag == null) {
            _uiState.value = _uiState.value.copy(itchGames = emptyList())
            return
        }

        // Only show global isLoading spinner when user explicitly selected ITCH_IO source.
        val showGlobalLoading = _uiState.value.selectedSource == SourceType.ITCH_IO
        viewModelScope.launch {
            if (showGlobalLoading) {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            }
            try {
                val games = itchClient.searchGamesByTag(tag)
                Log.d(TAG, "itch.io search: ${games.size} games for tag=$tag")
                val newState = _uiState.value.copy(itchGames = games)
                _uiState.value = if (showGlobalLoading) newState.copy(isLoading = false) else newState
            } catch (e: Exception) {
                Log.e(TAG, "itch.io search error", e)
                // Only publish error to UI if user is on itch.io chip; otherwise fail silently so
                // Archive.org / Local / SMB views are not affected by background itch.io failures.
                if (showGlobalLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }
    
    fun onItchGameSelected(game: ItchGame) {
        _uiState.value = _uiState.value.copy(
            selectedItchGame = game,
            itchUploads = emptyList(),
            isLoadingFiles = true
        )
        loadItchUploads(game)
    }
    
    private fun loadItchUploads(game: ItchGame) {
        val apiKey = itchKeyManager.getApiKey()
        if (apiKey == null) {
            _uiState.value = _uiState.value.copy(
                isLoadingFiles = false,
                error = "ITCH_API_KEY_REQUIRED"
            )
            return
        }
        
        viewModelScope.launch {
            try {
                // Resolve game ID from URL
                val gameId = itchClient.getGameIdFromUrl(game.url)
                if (gameId == null) {
                    Log.e(TAG, "Could not resolve game ID for ${game.url}")
                    _uiState.value = _uiState.value.copy(isLoadingFiles = false)
                    return@launch
                }
                
                val uploads = itchClient.listUploads(gameId, apiKey)
                Log.d(TAG, "Found ${uploads.size} ROM uploads for ${game.title}")

                // Mark game as fully downloaded when every upload file already exists in the library.
                val existing = romDownloader.checkBatchFilesExistence(
                    game.systemId,
                    uploads.map { it.filename }
                )
                val isFullyDownloaded = uploads.isNotEmpty() && existing.size == uploads.size
                val newFullSet = _uiState.value.fullyDownloadedPackIds.toMutableSet().apply {
                    if (isFullyDownloaded) add(game.id) else remove(game.id)
                }

                _uiState.value = _uiState.value.copy(
                    itchUploads = uploads,
                    isLoadingFiles = false,
                    fullyDownloadedPackIds = newFullSet
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error loading itch.io uploads", e)
                _uiState.value = _uiState.value.copy(isLoadingFiles = false)
            }
        }
    }
    
    fun clearSelectedItchGame() {
        _uiState.value = _uiState.value.copy(
            selectedItchGame = null,
            itchUploads = emptyList()
        )
    }
    
    fun startItchDownload(game: ItchGame, upload: ItchUpload) {
        val apiKey = itchKeyManager.getApiKey()
        if (apiKey == null) {
            _uiState.value = _uiState.value.copy(error = "ITCH_API_KEY_REQUIRED")
            return
        }
        
        Log.d(TAG, "Starting itch.io download: ${upload.filename} from ${game.title}")
        try {
            romDownloader.startItchIoDownload(game, upload, apiKey, itchClient)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start itch.io download", e)
            _uiState.value = _uiState.value.copy(error = e.message)
        }
    }
    
    fun toggleDownloadsPanel() {
        _uiState.value = _uiState.value.copy(showDownloadsPanel = !_uiState.value.showDownloadsPanel)
    }
    
    data class UiState(
        val selectedSystem: String = "snes",
        val searchQuery: String = "",
        val selectedRegion: String = "",
        val sortOption: SortOption = SortOption.DOWNLOADS,
        val selectedSource: SourceType = SourceType.ARCHIVE_ORG,
        val packs: List<ArchiveOrgClient.RomPack> = emptyList(),
        val filteredPacks: List<ArchiveOrgClient.RomPack> = emptyList(),
        val itchGames: List<ItchGame> = emptyList(),
        val selectedItchGame: ItchGame? = null,
        val itchUploads: List<ItchUpload> = emptyList(),
        val totalResults: Int = 0,
        val currentPage: Int = 1,
        val hasMorePages: Boolean = false,
        val isLoading: Boolean = false,
        val isLoadingMore: Boolean = false,
        val error: String? = null,
        val selectedPack: ArchiveOrgClient.RomPack? = null,
        val downloadableFiles: List<ArchiveOrgClient.DownloadableFile> = emptyList(),
        val downloadedFiles: Set<String> = emptySet(), // V8.6: ANR Fix - Pre-calculated state
        val isLoadingFiles: Boolean = false,
        val showDownloadsPanel: Boolean = false,
        val storageCheckResult: StorageCheckResult? = null,
        val itchApiKeyConfigured: Boolean = false,
        val itchUnsupportedSystem: Boolean = false,
        // Ids of packs/games whose files are ALL present in the library.
        // Populated lazily: Archive.org when the pack is opened; itch.io when a game is opened or downloaded.
        val fullyDownloadedPackIds: Set<String> = emptySet()
    )
    
    class Factory(
        private val context: Context,
        private val gameMetadataProvider: com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider,
        private val romDownloader: RomDownloader
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CatalogViewModel(context, gameMetadataProvider, romDownloader) as T
        }
    }
}

package com.swordfish.lemuroid.app.mobile.feature.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.app.shared.settings.StorageFrameworkPickerLauncher
import com.swordfish.lemuroid.common.coroutines.combine
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

@OptIn(FlowPreview::class)
class HomeViewModel(
    private val appContext: Context,
    retrogradeDb: RetrogradeDatabase,
    private val coresSelection: CoresSelection,
) : ViewModel() {
    companion object {
        const val CAROUSEL_MAX_ITEMS = 10
        const val HOME_PAGE_SIZE = 20
        const val HOME_PREFETCH_DISTANCE = 10
        const val HOME_MAX_LOADED_ITEMS = 60
        const val DEBOUNCE_TIME = 100L
    }

    class Factory(
        val appContext: Context,
        val retrogradeDb: RetrogradeDatabase,
        val coresSelection: CoresSelection,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(appContext, retrogradeDb, coresSelection) as T
        }
    }

    data class UIState(
        val favoritesGames: List<Game> = emptyList(),
        val recentGames: List<Game> = emptyList(),
        val discoveryGames: List<Game> = emptyList(),
        val indexInProgress: Boolean = true,
        val showNoNotificationPermissionCard: Boolean = false,
        val showNoMicrophonePermissionCard: Boolean = false,
        val showNoGamesCard: Boolean = false,
        val showStorageFolderInaccessibleCard: Boolean = false,
        val showDesmumeDeprecatedCard: Boolean = false,
    )

    private val microphonePermissionEnabledState = MutableStateFlow(true)
    private val notificationsPermissionEnabledState = MutableStateFlow(true)
    private val uiStates = MutableStateFlow(UIState())

    val homeGames: Flow<PagingData<Game>> =
        Pager(
            config = PagingConfig(
                pageSize = HOME_PAGE_SIZE,
                prefetchDistance = HOME_PREFETCH_DISTANCE,
                enablePlaceholders = true,
                maxSize = HOME_MAX_LOADED_ITEMS,
            ),
            pagingSourceFactory = { retrogradeDb.gameDao().selectHomeGames() },
        ).flow.cachedIn(viewModelScope)

    fun getViewStates(): Flow<UIState> {
        return uiStates
    }

    fun changeLocalStorageFolder(context: Context) {
        if (com.swordfish.lemuroid.app.tv.shared.TVHelper.isTV(context)) {
            com.swordfish.lemuroid.app.tv.folderpicker.TVFolderPickerLauncher.pickFolder(context)
        } else {
            StorageFrameworkPickerLauncher.pickFolder(context)
        }
    }

    fun updatePermissions(context: Context) {
        notificationsPermissionEnabledState.value = isNotificationsPermissionGranted(context)
        microphonePermissionEnabledState.value = isMicrophonePermissionGranted(context)
    }

    private fun isNotificationsPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        val permissionResult =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            )

        return permissionResult == PackageManager.PERMISSION_GRANTED
    }

    private fun isMicrophonePermissionGranted(context: Context): Boolean {
        val permissionResult =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO,
            )

        return permissionResult == PackageManager.PERMISSION_GRANTED
    }

    private fun buildViewState(
        favoritesGames: List<Game>,
        recentGames: List<Game>,
        discoveryGames: List<Game>,
        indexInProgress: Boolean,
        notificationsPermissionEnabled: Boolean,
        showMicrophoneCard: Boolean,
        showDesmumeWarning: Boolean,
    ): UIState {
        val noGames = recentGames.isEmpty() && favoritesGames.isEmpty() && discoveryGames.isEmpty()
        val storageFolderInaccessible = noGames && isConfiguredStorageInaccessible(appContext)

        return UIState(
            favoritesGames = favoritesGames,
            recentGames = recentGames,
            discoveryGames = discoveryGames,
            indexInProgress = indexInProgress,
            showNoNotificationPermissionCard = !notificationsPermissionEnabled,
            showNoMicrophonePermissionCard = showMicrophoneCard,
            showNoGamesCard = noGames && !storageFolderInaccessible,
            showStorageFolderInaccessibleCard = storageFolderInaccessible,
            showDesmumeDeprecatedCard = showDesmumeWarning,
        )
    }

    private fun isConfiguredStorageInaccessible(context: Context): Boolean {
        SharedPreferencesHelper.getSAFUri(context)?.let { uriString ->
            return !isSafFolderAccessible(context, uriString)
        }

        return SharedPreferencesHelper.getLegacyFolderPath(context)
            ?.let { path -> !isLegacyFolderAccessible(path) }
            ?: false
    }

    private fun isSafFolderAccessible(context: Context, uriString: String): Boolean {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return false
        if (uri.scheme == "file") {
            return uri.path?.let(::isLegacyFolderAccessible) ?: false
        }

        val hasReadPermission = context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isReadPermission }

        if (!hasReadPermission) return false

        return runCatching {
            val documentFile = DocumentFile.fromTreeUri(context, uri)
            documentFile != null && documentFile.exists() && documentFile.isDirectory && documentFile.canRead()
        }.getOrDefault(false)
    }

    private fun isLegacyFolderAccessible(path: String): Boolean {
        val folder = File(path)
        return folder.exists() && folder.isDirectory && folder.canRead()
    }

    init {
        viewModelScope.launch {
            val uiStatesFlow =
                combine(
                    favoritesGames(retrogradeDb),
                    recentGames(retrogradeDb),
                    discoveryGames(retrogradeDb),
                    indexingInProgress(appContext),
                    notificationsPermissionEnabledState,
                    microphoneNotification(retrogradeDb),
                    desmumeWarningNotification(),
                    ::buildViewState,
                )

            uiStatesFlow
                .debounce(DEBOUNCE_TIME)
                .flowOn(Dispatchers.IO)
                .collect { uiStates.value = it }
        }
    }

    private fun indexingInProgress(appContext: Context) =
        PendingOperationsMonitor(appContext).anyLibraryOperationInProgress()

    private fun discoveryGames(retrogradeDb: RetrogradeDatabase) =
        retrogradeDb.gameDao().selectFirstNotPlayed(CAROUSEL_MAX_ITEMS)
            .map { games ->
                timber.log.Timber.d("HomeViewModel: discoveryGames emitted ${games.size} games")
                games
            }

    private fun recentGames(retrogradeDb: RetrogradeDatabase) =
        retrogradeDb.gameDao().selectFirstUnfavoriteRecents(CAROUSEL_MAX_ITEMS)

    private fun favoritesGames(retrogradeDb: RetrogradeDatabase) =
        retrogradeDb.gameDao().selectFirstFavorites(CAROUSEL_MAX_ITEMS)

    private fun dsGamesCount(retrogradeDb: RetrogradeDatabase): Flow<Int> {
        return retrogradeDb.gameDao().selectSystemsWithCount()
            .map { systems ->
                systems
                    .firstOrNull { it.systemId == SystemID.NDS.dbname }
                    ?.count
                    ?: 0
            }
            .distinctUntilChanged()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun microphoneNotification(db: RetrogradeDatabase): Flow<Boolean> {
        return microphonePermissionEnabledState
            .flatMapLatest { isMicrophoneEnabled ->
                if (isMicrophoneEnabled) {
                    flowOf(false)
                } else {
                    combine(
                        coresSelection.getSelectedCores(),
                        dsGamesCount(db),
                    ) { cores, dsCount ->
                        cores.any { it.coreConfig.supportsMicrophone } &&
                            dsCount > 0
                    }
                }
                    .distinctUntilChanged()
            }
    }

    private fun desmumeWarningNotification(): Flow<Boolean> {
        return coresSelection.getSelectedCores()
            .map { cores -> cores.any { it.coreConfig.coreID == CoreID.DESMUME } }
            .distinctUntilChanged()
    }
}

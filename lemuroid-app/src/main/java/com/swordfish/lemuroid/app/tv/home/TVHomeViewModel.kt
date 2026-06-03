package com.swordfish.lemuroid.app.tv.home

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.library.PendingOperationsMonitor
import com.swordfish.lemuroid.app.shared.systems.MetaSystemInfo
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metaSystemID
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class TVHomeViewModel(retrogradeDb: RetrogradeDatabase, appContext: Context) : ViewModel() {
    companion object {
        const val CAROUSEL_MAX_ITEMS = 10
        const val DEBOUNCE_TIME = 100L
    }

    class Factory(
        val retrogradeDb: RetrogradeDatabase,
        val appContext: Context,
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TVHomeViewModel(retrogradeDb, appContext) as T
        }
    }

    data class HomeViewState(
        val favoritesGames: List<Game> = emptyList(),
        val recentGames: List<Game> = emptyList(),
        val metaSystems: List<MetaSystemInfo> = emptyList(),
        val indexInProgress: Boolean = false,
        val scanInProgress: Boolean = false,
        val metadataEnrichmentInProgress: Boolean = false,
        val hdModeEnabled: Boolean = false,
    )

    private val viewStates = MutableStateFlow(HomeViewState())

    fun getViewStates(): Flow<HomeViewState> {
        return viewStates
    }

    private fun buildViewState(
        favoritesGames: List<Game>,
        recentGames: List<Game>,
        metaSystems: List<MetaSystemInfo>,
        indexInProgress: Boolean,
        scanInProgress: Boolean,
        metadataEnrichmentInProgress: Boolean,
        hdModeEnabled: Boolean,
    ): HomeViewState {
        return HomeViewState(
            favoritesGames,
            recentGames,
            metaSystems,
            indexInProgress,
            scanInProgress,
            metadataEnrichmentInProgress,
            hdModeEnabled,
        )
    }

    init {
        viewModelScope.launch {
            val gamesFlow =
                combine(
                    favoriteGames(retrogradeDb),
                    recentGames(retrogradeDb),
                    availableSystems(retrogradeDb, appContext),
                    ::Triple,
                )
            val operationsFlow =
                combine(
                    indexingInProgress(appContext),
                    directoryScanInProgress(appContext),
                    metadataEnrichmentInProgress(appContext),
                    ::Triple,
                )
            val uiStatesFlow =
                combine(
                    gamesFlow,
                    operationsFlow,
                    hdModeEnabled(appContext),
                ) { games, operations, hdModeEnabled ->
                    buildViewState(
                        games.first,
                        games.second,
                        games.third,
                        operations.first,
                        operations.second,
                        operations.third,
                        hdModeEnabled,
                    )
                }

            uiStatesFlow
                .debounce(DEBOUNCE_TIME)
                .flowOn(Dispatchers.IO)
                .collect { viewStates.value = it }
        }
    }

    private fun directoryScanInProgress(appContext: Context) =
        PendingOperationsMonitor(appContext).isDirectoryScanInProgress()

    private fun indexingInProgress(appContext: Context) =
        PendingOperationsMonitor(appContext).anyLibraryOperationInProgress()

    private fun metadataEnrichmentInProgress(appContext: Context) =
        PendingOperationsMonitor(appContext).isMetadataEnrichmentInProgress()

    private fun hdModeEnabled(appContext: Context) =
        callbackFlow {
            val preferences = SharedPreferencesHelper.getSharedPreferences(appContext)
            val key = appContext.getString(R.string.pref_key_hd_mode)

            fun sendCurrentValue() {
                trySend(preferences.getBoolean(key, false))
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, changedKey ->
                    if (changedKey == key) {
                        sendCurrentValue()
                    }
                }

            sendCurrentValue()
            preferences.registerOnSharedPreferenceChangeListener(listener)
            awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
        }.distinctUntilChanged()

    private fun availableSystems(
        retrogradeDb: RetrogradeDatabase,
        appContext: Context,
    ) = retrogradeDb.gameDao()
        .selectSystemsWithCount()
        .map { systemCounts ->
            systemCounts.asSequence()
                .filter { (_, count) -> count > 0 }
                .map { (systemId, count) -> GameSystem.findById(systemId).metaSystemID() to count }
                .groupBy { (metaSystemId, _) -> metaSystemId }
                .map { (metaSystemId, counts) -> MetaSystemInfo(metaSystemId, counts.sumBy { it.second }) }
                .sortedBy { it.getName(appContext) }
                .toList()
        }

    private fun favoriteGames(retrogradeDb: RetrogradeDatabase) =
        retrogradeDb.gameDao()
            .selectFirstFavoritesRecents(CAROUSEL_MAX_ITEMS + 1)

    private fun recentGames(retrogradeDb: RetrogradeDatabase) =
        retrogradeDb.gameDao()
            .selectFirstUnfavoriteRecents(CAROUSEL_MAX_ITEMS)
}

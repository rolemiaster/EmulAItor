package com.swordfish.lemuroid.app.shared

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.SystemClock
import android.widget.Toast
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.mobile.feature.shortcuts.ShortcutsGenerator
import com.swordfish.lemuroid.app.shared.game.GameLauncher
import com.swordfish.lemuroid.app.shared.main.BusyActivity
import com.swordfish.lemuroid.common.displayDetailsSettingsScreen
import com.swordfish.lemuroid.common.displayToast
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.preferences.SharedPreferencesHelper
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GameInteractor(
    private val activity: BusyActivity,
    private val retrogradeDb: RetrogradeDatabase,
    private val useLeanback: Boolean,
    private val shortcutsGenerator: ShortcutsGenerator,
    private val gameLauncher: GameLauncher,
    private val lemuroidLibrary: LemuroidLibrary,
) {
    private var firstBusyLaunchAtMs: Long? = null
    private var busyDialogShowing = false

    fun onGamePlay(game: Game) {
        if (activity.isBusy()) {
            handleBusyLaunch()
            return
        }
        resetBusyLaunchTracking()
        gameLauncher.launchGameAsync(activity.activity(), game, true, useLeanback)
    }

    fun onGameRestart(game: Game) {
        if (activity.isBusy()) {
            handleBusyLaunch()
            return
        }
        resetBusyLaunchTracking()
        gameLauncher.launchGameAsync(activity.activity(), game, false, useLeanback)
    }

    private fun handleBusyLaunch() {
        val context = activity.activity()
        val elapsedMs = trackBusyLaunchDuration()

        if (elapsedMs < BUSY_WARNING_THRESHOLD_MS) {
            context.displayToast(R.string.game_interactory_busy, Toast.LENGTH_SHORT)
            return
        }

        showBlockedOperationsDialog(context)
    }

    private fun trackBusyLaunchDuration(): Long {
        val now = SystemClock.elapsedRealtime()
        val firstBusyAt = firstBusyLaunchAtMs ?: now.also { firstBusyLaunchAtMs = it }
        return now - firstBusyAt
    }

    private fun resetBusyLaunchTracking() {
        firstBusyLaunchAtMs = null
        busyDialogShowing = false
    }

    private fun showBlockedOperationsDialog(context: Activity) {
        if (busyDialogShowing || context.isFinishing || context.isDestroyed) {
            return
        }

        busyDialogShowing = true
        val romsAreExternal = romLibraryIsOutsideAppData(context)
        val dialog =
            android.app.AlertDialog.Builder(context)
                .setTitle(R.string.pending_operations_blocked_title)
                .setMessage(blockedOperationsMessage(context, romsAreExternal))
                .setNegativeButton(R.string.pending_operations_keep_waiting, null)

        if (romsAreExternal) {
            dialog.setPositiveButton(R.string.pending_operations_clear_data) { _, _ ->
                showClearAppDataConfirmation(context)
            }
        } else {
            dialog.setPositiveButton(R.string.settings) { _, _ ->
                context.displayDetailsSettingsScreen()
            }
        }

        dialog.setOnDismissListener { busyDialogShowing = false }
        dialog.show()
    }

    private fun blockedOperationsMessage(context: Context, romsAreExternal: Boolean): String {
        val messageId =
            if (romsAreExternal) {
                R.string.pending_operations_blocked_message_external
            } else {
                R.string.pending_operations_blocked_message_internal
            }

        return context.getString(messageId)
    }

    private fun romLibraryIsOutsideAppData(context: Context): Boolean {
        val preferences = SharedPreferencesHelper.getSharedPreferences(context)
        val libraryType = preferences.getString(SharedPreferencesHelper.KEY_LIBRARY_TYPE, null)
        val safUri = preferences.getString(SharedPreferencesHelper.KEY_STORAGE_FOLDER_URI, null)
        val legacyPath = SharedPreferencesHelper.getLegacyFolderPath(context)

        return libraryType == LIBRARY_TYPE_SMB || safUri != null || legacyPath != null
    }

    private fun clearAppData(context: Activity) {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val clearStarted = activityManager.clearApplicationUserData()
        if (!clearStarted) {
            context.displayToast(R.string.catalog_error_occurred, Toast.LENGTH_LONG)
        }
    }

    private fun showClearAppDataConfirmation(context: Activity) {
        if (context.isFinishing || context.isDestroyed) {
            return
        }

        android.app.AlertDialog.Builder(context)
            .setTitle(R.string.pending_operations_clear_data_confirm_title)
            .setMessage(R.string.pending_operations_clear_data_confirm_message)
            .setPositiveButton(R.string.pending_operations_clear_data_confirm_button) { _, _ ->
                clearAppData(context)
            }
            .setNegativeButton(R.string.pending_operations_keep_waiting, null)
            .show()
    }

    fun onFavoriteToggle(
        game: Game,
        isFavorite: Boolean,
    ) {
        GlobalScope.launch {
            retrogradeDb.gameDao().update(game.copy(isFavorite = isFavorite))
        }
    }

    fun onCreateShortcut(game: Game) {
        GlobalScope.launch {
            shortcutsGenerator.pinShortcutForGame(game)
        }
    }

    fun supportShortcuts(): Boolean {
        return shortcutsGenerator.supportShortcuts()
    }
    
    fun updateGame(game: Game) {
        GlobalScope.launch {
            retrogradeDb.gameDao().update(game)
        }
    }

    fun deleteGame(game: Game) {
        GlobalScope.launch {
            lemuroidLibrary.deleteGame(game)
        }
    }
    
    fun onDeleteGame(game: Game) {
        val context = activity.activity()
        android.app.AlertDialog.Builder(context)
            .setTitle(R.string.game_context_menu_delete)
            .setMessage(context.getString(R.string.game_delete_confirmation, game.title))
            .setPositiveButton(R.string.ok) { _, _ ->
                deleteGame(game)
                context.displayToast(R.string.game_deleted)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun deleteGames(games: List<Game>) {
        GlobalScope.launch {
            games.forEach { lemuroidLibrary.deleteGame(it) }
        }
    }

    fun onRenameGame(game: Game) {
        val context = activity.activity()
        val editText = android.widget.EditText(context).apply {
            setText(game.title)
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            val padding = (20 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        android.app.AlertDialog.Builder(context)
            .setTitle(R.string.game_context_menu_edit)
            .setView(editText)
            .setPositiveButton(R.string.game_edit_save) { _, _ ->
                val newTitle = editText.text.toString()
                if (newTitle.isNotBlank() && newTitle != game.title) {
                    updateGame(game.copy(
                        title = newTitle,
                        isUserLocked = true // Bloqueamos para que el indexador no lo sobrescriba
                    ))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    fun onChangeSystem(game: Game) {
        if (useLeanback) {
            // TV mode - launch TVGameEditActivity
            val intent = com.swordfish.lemuroid.app.tv.settings.TVGameEditActivity.createIntent(
                activity.activity(), 
                game
            )
            activity.activity().startActivityForResult(intent, REQUEST_CHANGE_SYSTEM)
        }
        // Mobile mode is handled differently (via GameEditDialog in Compose)
    }
    
    fun handleActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        if (requestCode == REQUEST_CHANGE_SYSTEM && resultCode == android.app.Activity.RESULT_OK) {
            val updatedGame = data?.getSerializableExtra("extra_game") as? Game
            if (updatedGame != null) {
                updateGame(updatedGame)
            }
        }
    }
    
    companion object {
        const val REQUEST_CHANGE_SYSTEM = 1001
        private const val BUSY_WARNING_THRESHOLD_MS = 60_000L
        private const val LIBRARY_TYPE_SMB = "smb"
    }
}

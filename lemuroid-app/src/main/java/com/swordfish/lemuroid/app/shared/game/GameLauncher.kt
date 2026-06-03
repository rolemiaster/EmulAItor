package com.swordfish.lemuroid.app.shared.game

import android.app.Activity
import com.swordfish.lemuroid.app.shared.main.GameLaunchTaskHandler
import com.swordfish.lemuroid.lib.core.CoresSelection
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.db.entity.Game
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class GameLauncher(
    private val coresSelection: CoresSelection,
    private val gameLaunchTaskHandler: GameLaunchTaskHandler,
) {
    @OptIn(DelicateCoroutinesApi::class)
    fun launchGameAsync(
        activity: Activity,
        game: Game,
        loadSave: Boolean,
        leanback: Boolean,
    ) {
        GlobalScope.launch {
            try {
                val system = GameSystem.findById(game.systemId)
                val coreConfig = coresSelection.getCoreConfigForSystem(system)
                if (!gameLaunchTaskHandler.checkAccessAndStart(activity)) {
                    return@launch
                }
                BaseGameActivity.launchGame(activity, coreConfig, game, loadSave, leanback)
            } catch (e: Exception) {
                e.printStackTrace()
                // Show friendly error message instead of crashing
                activity.runOnUiThread {
                    com.swordfish.lemuroid.app.shared.gamecrash.GameCrashActivity.launch(
                        activity,
                        activity.getString(com.swordfish.lemuroid.lib.R.string.error_game_launch_failed),
                        "System: ${game.systemId}\nError: ${e.message}\n\nMost likely the game file is corrupted or the system is not properly recognized."
                    )
                }
            }
        }
    }
}

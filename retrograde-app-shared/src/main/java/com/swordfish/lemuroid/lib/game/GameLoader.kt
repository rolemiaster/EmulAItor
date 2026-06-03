/*
 * GameLoader.kt
 *
 * Copyright (C) 2017 Retrograde Project
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.swordfish.lemuroid.lib.game

import android.content.Context
import android.os.Build
import com.swordfish.lemuroid.lib.bios.BiosManager
import com.swordfish.lemuroid.lib.core.CoreVariable
import com.swordfish.lemuroid.lib.core.CoreVariablesManager
import com.swordfish.lemuroid.lib.library.CoreID
import com.swordfish.lemuroid.lib.library.GameSystem
import com.swordfish.lemuroid.lib.library.LemuroidLibrary
import com.swordfish.lemuroid.lib.library.SystemCoreConfig
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.migration.DesmumeMigrationHandler
import com.swordfish.lemuroid.lib.saves.SaveState
import com.swordfish.lemuroid.lib.saves.SavesCoherencyEngine
import com.swordfish.lemuroid.lib.saves.SavesManager
import com.swordfish.lemuroid.lib.saves.StatesManager
import com.swordfish.lemuroid.lib.storage.DirectoriesManager
import com.swordfish.lemuroid.lib.storage.RomFiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class GameLoader(
    private val lemuroidLibrary: LemuroidLibrary,
    private val statesManager: StatesManager,
    private val savesManager: SavesManager,
    private val coreVariablesManager: CoreVariablesManager,
    private val retrogradeDatabase: RetrogradeDatabase,
    private val savesCoherencyEngine: SavesCoherencyEngine,
    private val directoriesManager: DirectoriesManager,
    private val biosManager: BiosManager,
    private val desmumeMigrationHandler: DesmumeMigrationHandler,
) {
    sealed class LoadingState {
        object LoadingCore : LoadingState()

        object LoadingGame : LoadingState()

        class Ready(val gameData: GameData) : LoadingState()
    }

    fun load(
        appContext: Context,
        game: Game,
        loadSave: Boolean,
        systemCoreConfig: SystemCoreConfig,
        directLoad: Boolean,
    ): Flow<LoadingState> =
        flow {
            try {
                emit(LoadingState.LoadingCore)

                val system = GameSystem.findById(game.systemId)

                if (!isArchitectureSupported(systemCoreConfig)) {
                    throw GameLoaderException(GameLoaderError.UnsupportedArchitecture)
                }

                val coreLibrary =
                    runCatching {
                        findLibrary(appContext, systemCoreConfig.coreID)!!.absolutePath
                    }.getOrElse { throw GameLoaderException(GameLoaderError.LoadCore) }

                emit(LoadingState.LoadingGame)

                val missingBiosFiles = biosManager.getMissingBiosFiles(systemCoreConfig, game)
                if (missingBiosFiles.isNotEmpty()) {
                    throw GameLoaderException(GameLoaderError.MissingBiosFiles(missingBiosFiles))
                }

                val gameFiles =
                    runCatching {
                        val useVFS = systemCoreConfig.supportsLibretroVFS && directLoad
                        val dataFiles = retrogradeDatabase.dataFileDao().selectDataFilesForGame(game.id)
                        lemuroidLibrary.getGameFiles(game, dataFiles, useVFS)
                    }.getOrElse { throw it }

                val saveRAM =
                    runCatching {
                        val data = savesManager.getSaveRAM(game, systemCoreConfig)
                        desmumeMigrationHandler.resolveSaveData(game, systemCoreConfig.coreID, data)
                    }.getOrElse { throw GameLoaderException(GameLoaderError.Saves) }
                val saveRAMData = saveRAM.data

                val quickSaveData =
                    runCatching {
                        val shouldDiscardSave =
                            !savesCoherencyEngine.shouldDiscardAutoSaveState(
                                game,
                                systemCoreConfig.coreID,
                                saveRAM.timestampOverride,
                            )

                        if (systemCoreConfig.statesSupported && loadSave && shouldDiscardSave) {
                            statesManager.getAutoSave(game, systemCoreConfig.coreID)
                        } else {
                            null
                        }
                    }.getOrElse { throw GameLoaderException(GameLoaderError.Saves) }

                val coreVariables =
                    coreVariablesManager.getOptionsForCore(system.id, systemCoreConfig)
                        .toTypedArray()

                val systemDirectory = directoriesManager.getSystemDirectory()
                val savesDirectory = directoriesManager.getSavesDirectory()

                emit(
                    LoadingState.Ready(
                        GameData(
                            game,
                            coreLibrary,
                            gameFiles,
                            quickSaveData,
                            saveRAMData,
                            coreVariables,
                            systemDirectory,
                            savesDirectory,
                        ),
                    ),
                )
            } catch (e: GameLoaderException) {
                Timber.e(e, "Error while preparing game")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error while preparing game")
                throw GameLoaderException(GameLoaderError.Generic)
            }
        }

    private fun isArchitectureSupported(systemCoreConfig: SystemCoreConfig): Boolean {
        val supportedOnlyArchitectures = systemCoreConfig.supportedOnlyArchitectures ?: return true
        return Build.SUPPORTED_ABIS.toSet().intersect(supportedOnlyArchitectures).isNotEmpty()
    }

    private fun findLibrary(
        context: Context,
        coreID: CoreID,
    ): File? {
        val files =
            sequenceOf(
                File(context.applicationInfo.nativeLibraryDir),
                context.filesDir,
            )

        return files
            .flatMap { it.walkBottomUp() }
            .firstOrNull { it.name == coreID.libretroFileName }
            ?: extractBundledCore(context, coreID)
    }

    private fun extractBundledCore(context: Context, coreID: CoreID): File? {
        val libName = coreID.libretroFileName
        val cacheDir = File(context.codeCacheDir, "bundled_cores")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }

        val destinationFile = File(cacheDir, libName)
        if (destinationFile.exists() && destinationFile.length() > 0) {
            return destinationFile
        }

        // Collect all APK paths (Base + Splits)
        val apkPaths = mutableListOf<String>()
        context.applicationInfo.sourceDir?.let { apkPaths.add(it) }
        context.applicationInfo.splitSourceDirs?.let { apkPaths.addAll(it) }

        Timber.i("Searching for $libName in APK paths: ${apkPaths.joinToString()}")

        for (apkPath in apkPaths) {
            try {
                ZipFile(apkPath).use { zip ->
                    val abis = Build.SUPPORTED_ABIS
                    var entryName: String? = null
                    
                    for (abi in abis) {
                        val candidate = "lib/$abi/$libName"
                        if (zip.getEntry(candidate) != null) {
                            entryName = candidate
                            break
                        }
                    }

                    if (entryName != null) {
                        val entry = zip.getEntry(entryName)
                        zip.getInputStream(entry).use { input ->
                            FileOutputStream(destinationFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        destinationFile.setExecutable(true, false)
                        Timber.i("Found and extracted $libName from $apkPath")
                        return destinationFile
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to read APK $apkPath during search")
            }
        }
        
        Timber.e("Core $libName not found in any APK split")
        return null
    }

    @Suppress("ArrayInDataClass")
    data class GameData(
        val game: Game,
        val coreLibrary: String,
        val gameFiles: RomFiles,
        val quickSaveData: SaveState?,
        val saveRAMData: ByteArray?,
        val coreVariables: Array<CoreVariable>,
        val systemDirectory: File,
        val savesDirectory: File,
    )
}

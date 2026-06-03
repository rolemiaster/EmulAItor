package com.swordfish.lemuroid.app.shared.library

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.swordfish.lemuroid.app.mobile.feature.metadata.TheGamesDBMetadataProvider
import com.swordfish.lemuroid.lib.injection.AndroidWorkerInjection
import com.swordfish.lemuroid.lib.injection.WorkerKey
import com.swordfish.lemuroid.lib.library.db.RetrogradeDatabase
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.storage.StorageFile
import dagger.Binds
import dagger.android.AndroidInjector
import dagger.multibindings.IntoMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class LibraryMetadataEnrichmentWork(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    @Inject
    lateinit var retrogradeDatabase: RetrogradeDatabase

    @Inject
    lateinit var theGamesDBMetadataProvider: TheGamesDBMetadataProvider

    override suspend fun doWork(): Result {
        AndroidWorkerInjection.inject(this)

        return withContext(Dispatchers.IO) {
            runCatching {
                enrichMissingMetadata()
                Result.success()
            }.getOrElse { error ->
                Timber.e(error, "Library metadata enrichment terminated with an exception")
                Result.retry()
            }
        }
    }

    private suspend fun enrichMissingMetadata() {
        val prefs =
            applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val attemptedUris = prefs.getStringSet(KEY_ATTEMPTED_URIS, emptySet()).orEmpty().toMutableSet()

        val candidates =
            retrogradeDatabase.gameDao()
                .selectGamesNeedingMetadataEnrichment(MAX_GAMES_PER_RUN)
                .filterNot { attemptedUris.contains(it.fileUri) }

        Timber.i("Library metadata enrichment starting for ${candidates.size} games")

        candidates.forEach { game ->
            val metadata =
                theGamesDBMetadataProvider.retrieveMetadata(
                    StorageFile(
                        name = game.fileName,
                        size = 0L,
                        uri = Uri.parse(game.fileUri),
                    ),
                )

            val enrichedGame = metadata?.let { enriched ->
                game.copy(
                    developer = game.developer ?: enriched.developer,
                    coverFrontUrl = game.coverFrontUrl ?: enriched.thumbnail,
                    year = game.year ?: enriched.year,
                    genre = game.genre ?: enriched.genre,
                    description = game.description ?: enriched.description,
                    publisher = game.publisher ?: enriched.publisher,
                )
            }

            if (enrichedGame != null && enrichedGame != game) {
                retrogradeDatabase.gameDao().update(enrichedGame)
                Timber.d("Library metadata enriched for: ${game.title}")
            }

            attemptedUris.add(game.fileUri)
            prefs.edit().putStringSet(KEY_ATTEMPTED_URIS, attemptedUris).apply()
        }

        Timber.i("Library metadata enrichment completed")
    }

    @dagger.Module(subcomponents = [Subcomponent::class])
    abstract class Module {
        @Binds
        @IntoMap
        @WorkerKey(LibraryMetadataEnrichmentWork::class)
        abstract fun bindMyWorkerFactory(builder: Subcomponent.Builder): AndroidInjector.Factory<out ListenableWorker>
    }

    @dagger.Subcomponent
    interface Subcomponent : AndroidInjector<LibraryMetadataEnrichmentWork> {
        @dagger.Subcomponent.Builder
        abstract class Builder : AndroidInjector.Builder<LibraryMetadataEnrichmentWork>()
    }

    companion object {
        private const val PREFS_NAME = "library_metadata_enrichment"
        private const val KEY_ATTEMPTED_URIS = "attempted_uris"
        private const val MAX_GAMES_PER_RUN = 100
    }
}

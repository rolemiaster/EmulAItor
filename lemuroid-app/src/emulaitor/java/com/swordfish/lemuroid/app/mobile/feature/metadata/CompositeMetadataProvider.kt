package com.swordfish.lemuroid.app.mobile.feature.metadata

import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider
import com.swordfish.lemuroid.lib.storage.StorageFile
import timber.log.Timber

/**
 * Metadata provider used by the blocking library scan.
 *
 * The scan must stay local and quick so users can launch already indexed games while
 * richer online metadata is refreshed by LibraryMetadataEnrichmentWork.
 */
class CompositeMetadataProvider(
    private val systemDetectionProviders: List<GameMetadataProvider>,
    private val enrichmentProviders: List<GameMetadataProvider> = emptyList()
) : GameMetadataProvider {

    override suspend fun retrieveMetadata(storageFile: StorageFile): GameMetadata? {
        // Phase 1: System Detection - Get basic metadata with system identification
        var detectedMetadata: GameMetadata? = null
        
        for (provider in systemDetectionProviders) {
            try {
                val metadata = provider.retrieveMetadata(storageFile)
                if (metadata != null && !metadata.system.isNullOrEmpty()) {
                    Timber.d("System detected by ${provider.javaClass.simpleName} for ${storageFile.name}: ${metadata.system}")
                    detectedMetadata = metadata
                    break
                }
            } catch (e: Exception) {
                Timber.w(e, "Provider ${provider.javaClass.simpleName} failed for ${storageFile.name}")
            }
        }
        
        // Fallback: Try to identify system by unique file extension
        if (detectedMetadata == null) {
            val extension = storageFile.name.substringAfterLast('.', "")
            if (extension.isNotEmpty()) {
                val system = com.swordfish.lemuroid.lib.library.GameSystem.findByUniqueFileExtension(extension)
                if (system != null) {
                    Timber.d("System identified by extension for ${storageFile.name}: ${system.id}")
                    detectedMetadata = GameMetadata(
                        name = storageFile.extensionlessName,
                        description = null,
                        thumbnail = null,
                        system = system.id.dbname,
                        publisher = null,
                        developer = null,
                        genre = null,
                        year = null,
                        romName = storageFile.name
                    )
                }
            }
        }
        
        // Final fallback: Unknown system
        if (detectedMetadata == null) {
            detectedMetadata = GameMetadata(
                name = storageFile.name,
                description = "Unknown System",
                thumbnail = null,
                system = com.swordfish.lemuroid.lib.library.SystemID.UNKNOWN.dbname,
                publisher = null,
                developer = null,
                genre = null,
                year = null,
                romName = storageFile.name
            )
        }
        
        return detectedMetadata
    }
    
    override suspend fun searchByName(name: String): List<GameMetadata> {
        val results = mutableListOf<GameMetadata>()
        for (provider in enrichmentProviders) {
            try {
                results.addAll(provider.searchByName(name))
            } catch (e: Exception) {
                Timber.w(e, "Enrichment provider ${provider.javaClass.simpleName} search failed for $name")
            }
        }
        return results
    }

}

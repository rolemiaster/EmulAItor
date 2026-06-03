package com.swordfish.lemuroid.app.mobile.feature.catalog

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.app.shared.library.LibraryIndexScheduler
import kotlinx.coroutines.launch

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    modifier: Modifier = Modifier,
    gameMetadataProvider: com.swordfish.lemuroid.lib.library.metadata.GameMetadataProvider,
    romDownloader: RomDownloader,
    viewModel: CatalogViewModel = viewModel(factory = CatalogViewModel.Factory(LocalContext.current.applicationContext, gameMetadataProvider, romDownloader))
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()
    val downloads by viewModel.downloads.collectAsState()
    val invalidCatalogDownload = downloads.values.firstOrNull { it.requiresUserAttention }
    
    // Source management
    val sourceManager = remember { SourceManager(context) }
    var sources by remember { mutableStateOf(sourceManager.getSources()) }
    var selectedSourceType by remember { mutableStateOf<SourceType?>(null) } // null = all
    
    // Dialog states
    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showManageSourcesDialog by remember { mutableStateOf(false) }
    var showItchApiKeyDialog by remember { mutableStateOf(false) }
    var showItchApiKeyRequiredDialog by remember { mutableStateOf(false) }
    
    // State for reloading external files
    var shouldReloadLocalFiles by remember { mutableStateOf(true) }
    
    // SAF Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            try {
                context.contentResolver.takePersistableUriPermission(
                    selectedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                Log.w("CatalogScreen", "Unable to persist local source permission for $selectedUri", e)
                Toast.makeText(
                    context,
                    context.getString(R.string.sources_local_permission_error),
                    Toast.LENGTH_LONG,
                ).show()
                return@let
            }

            // Get folder name
            val folderName = DocumentFile.fromTreeUri(context, selectedUri)?.name ?: "Local Folder"

            // Add source
            val newSource = RomSource.local(folderName, selectedUri.toString())
            sourceManager.addSource(newSource)
            sources = sourceManager.getSources()
            
            // Trigger reload of local files
            shouldReloadLocalFiles = true
        }
    }
    
    // States for Local/SMB files
    var localFiles by remember { mutableStateOf<List<LocalFile>>(emptyList()) }
    var smbFiles by remember { mutableStateOf<List<Pair<RomSource, SmbFile>>>(emptyList()) }
    var isLoadingExternalFiles by remember { mutableStateOf(false) }
    
    // SMB download states
    var smbDownloadsInProgress by remember { mutableStateOf<Set<String>>(emptySet()) }
    var smbDownloadedFiles by remember { mutableStateOf<Set<String>>(emptySet()) }
    val romDownloader = viewModel.romDownloader
    
    // Load files from Local and SMB sources
    val localScanner = remember { LocalFolderScanner(context) }
    val smbClient = remember { SmbClient() }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(sources, shouldReloadLocalFiles) {
        if (shouldReloadLocalFiles) {
            isLoadingExternalFiles = true
            
            // Load local files
            val allLocalFiles = mutableListOf<LocalFile>()
            sources.filter { it.type == SourceType.LOCAL }.forEach { source ->
                try {
                    val uri = Uri.parse(source.path)
                    val result = localScanner.scanFolder(uri)
                    result.onSuccess { files ->
                        allLocalFiles.addAll(files)
                    }
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
            localFiles = allLocalFiles
            
            // Load SMB files
            val allSmbFiles = mutableListOf<Pair<RomSource, SmbFile>>()
            sources.filter { it.type == SourceType.SMB }.forEach { source ->
                try {
                    // Parse SMB path: smb://server/sharename/optional/subpath
                    // El path ya incluye el sharename como primera parte
                    val smbPath = source.path.removePrefix("smb://")
                    val slashIndex = smbPath.indexOf('/')
                    if (slashIndex <= 0) {
                        Log.w("CatalogScreen", "Invalid SMB path: ${source.path}")
                        return@forEach
                    }
                    
                    val server = smbPath.substring(0, slashIndex)
                    val fullPath = smbPath.substring(slashIndex) // /sharename/subpath
                    
                    // Extraer sharename (primera parte del path) y subpath (resto)
                    val pathParts = fullPath.removePrefix("/").split("/", limit = 2)
                    val share = pathParts.getOrNull(0) ?: return@forEach
                    val subPath = if (pathParts.size > 1) pathParts[1] else ""
                    
                    Log.d("CatalogScreen", "SMB: server=$server, share=$share, subPath=$subPath")
                    
                    val result = smbClient.listFiles(server, share, subPath, source.credentials)
                    result.onSuccess { files ->
                        Log.d("CatalogScreen", "SMB found ${files.size} files")
                        files.forEach { file ->
                            allSmbFiles.add(source to file)
                        }
                    }.onFailure { e ->
                        Log.e("CatalogScreen", "SMB error: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.e("CatalogScreen", "SMB exception: ${e.message}", e)
                }
            }
            smbFiles = allSmbFiles
            
            isLoadingExternalFiles = false
            shouldReloadLocalFiles = false
        }
    }

    // V8.6 ANR FIX: Check SMB files existence in background, not in UI composition
    LaunchedEffect(smbFiles) {
        if (smbFiles.isNotEmpty()) {
            with(kotlinx.coroutines.Dispatchers.IO) {
                // Batch check (simplistic iteration but off-thread)
                val existingPaths = smbFiles
                    .filter { (_, file) -> romDownloader.isFileInRomsDir(file.name) }
                    .map { (_, file) -> file.path }
                    .toSet()
                
                if (existingPaths.isNotEmpty()) {
                     // Main thread update is automatic via MutableState
                     smbDownloadedFiles = smbDownloadedFiles + existingPaths
                }
            }
        }
    }
    
    // Derived: should show only Archive.org packs or filter by source type
    val showArchiveOrgContent = selectedSourceType == null || selectedSourceType == SourceType.ARCHIVE_ORG
    val showItchContent = selectedSourceType == null || selectedSourceType == SourceType.ITCH_IO
    val showLocalContent = selectedSourceType == null || selectedSourceType == SourceType.LOCAL
    val showSmbContent = selectedSourceType == null || selectedSourceType == SourceType.SMB

    // Always refresh itch.io API key state on screen entry
    LaunchedEffect(Unit) {
        viewModel.refreshItchApiKeyState()
    }

    // When source chip changes, trigger appropriate search
    LaunchedEffect(selectedSourceType) {
        if (selectedSourceType == SourceType.ITCH_IO) {
            viewModel.refreshItchApiKeyState()
            viewModel.selectSource(SourceType.ITCH_IO)
        }
    }

    // When system changes or "All" chip is active, auto-load itch.io games in background
    LaunchedEffect(uiState.selectedSystem, selectedSourceType) {
        if (showItchContent && uiState.itchGames.isEmpty() && !uiState.itchUnsupportedSystem) {
            viewModel.searchItchGamesIfNeeded()
        }
    }

    // Handle ITCH_API_KEY_REQUIRED error
    LaunchedEffect(uiState.error) {
        if (uiState.error == "ITCH_API_KEY_REQUIRED") {
            showItchApiKeyRequiredDialog = true
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Disclaimer Banner
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.catalog_disclaimer),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Sources row with Add button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Source type chips
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // All sources
                    item {
                        FilterChip(
                            selected = selectedSourceType == null,
                            onClick = { selectedSourceType = null },
                            label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    // Cloud (Archive.org)
                    item {
                        FilterChip(
                            selected = selectedSourceType == SourceType.ARCHIVE_ORG,
                            onClick = {
                                selectedSourceType = SourceType.ARCHIVE_ORG
                                viewModel.selectSource(SourceType.ARCHIVE_ORG)
                            },
                            leadingIcon = { Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp)) },
                            label = { Text(stringResource(R.string.catalog_filter_cloud), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    // itch.io Homebrew
                    item {
                        FilterChip(
                            selected = selectedSourceType == SourceType.ITCH_IO,
                            onClick = {
                                selectedSourceType = SourceType.ITCH_IO
                                viewModel.selectSource(SourceType.ITCH_IO)
                            },
                            leadingIcon = { Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(16.dp)) },
                            label = { Text(stringResource(R.string.itch_filter_label), style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    // Local
                    if (sources.any { it.type == SourceType.LOCAL }) {
                        item {
                            FilterChip(
                                selected = selectedSourceType == SourceType.LOCAL,
                                onClick = { selectedSourceType = SourceType.LOCAL },
                                leadingIcon = { Icon(Icons.Default.Folder, null, modifier = Modifier.size(16.dp)) },
                                label = { Text(stringResource(R.string.catalog_filter_local), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    // SMB
                    if (sources.any { it.type == SourceType.SMB }) {
                        item {
                            FilterChip(
                                selected = selectedSourceType == SourceType.SMB,
                                onClick = { selectedSourceType = SourceType.SMB },
                                leadingIcon = { Icon(Icons.Default.Dns, null, modifier = Modifier.size(16.dp)) },
                                label = { Text(stringResource(R.string.catalog_filter_smb), style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                
                // Add Source button
                IconButton(onClick = { showAddSourceDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.sources_add_title), tint = MaterialTheme.colorScheme.onSurface)
                }
                
                // Manage Sources button (only if custom - non built-in - sources exist)
                if (sources.any { it.type != SourceType.ARCHIVE_ORG && it.type != SourceType.ITCH_IO }) {
                    IconButton(onClick = { showManageSourcesDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.sources_manage_title), tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            
            // Chips de sistemas
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(viewModel.availableSystems) { (id, name) ->
                    FilterChip(
                        selected = uiState.selectedSystem == id,
                        onClick = { viewModel.selectSystem(id) },
                        label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            
            // Barra de búsqueda + filtro región
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { 
                        focusManager.clearFocus()
                        viewModel.searchPacks()  // Execute search when user presses Enter
                    }),
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent {
                            if (it.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN && it.type == androidx.compose.ui.input.key.KeyEventType.KeyDown) {
                                focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                                true
                            } else {
                                false
                            }
                        }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Region dropdown
                var regionExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = uiState.selectedRegion.isNotEmpty(),
                        onClick = { regionExpanded = true },
                        label = { 
                            Text(
                                if (uiState.selectedRegion.isEmpty()) "🌍" else getRegionDisplayName(uiState.selectedRegion).take(6),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    DropdownMenu(expanded = regionExpanded, onDismissRequest = { regionExpanded = false }) {
                        viewModel.availableRegions.forEach { regionCode ->
                            DropdownMenuItem(
                                text = { Text(getRegionDisplayName(regionCode)) },
                                onClick = {
                                    viewModel.setRegionFilter(regionCode)
                                    regionExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Info de resultados + ordenación + botón descargas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (selectedSourceType == SourceType.ITCH_IO) {
                        stringResource(R.string.catalog_games_count, uiState.itchGames.size)
                    } else {
                        stringResource(R.string.catalog_packs_count, uiState.filteredPacks.size, uiState.totalResults)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sort options
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { sortExpanded = true }) {
                            Icon(Icons.Default.FilterList, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                getSortOptionDisplayName(uiState.sortOption),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                            viewModel.sortOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(getSortOptionDisplayName(option)) },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        sortExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Botón descargas
                    val activeDownloads = downloads.count { 
                        it.value.status == RomDownloader.DownloadStatus.DOWNLOADING || 
                        it.value.status == RomDownloader.DownloadStatus.PENDING 
                    }
                    BadgedBox(
                        badge = { if (activeDownloads > 0) Badge { Text("$activeDownloads") } }
                    ) {
                        IconButton(onClick = { viewModel.toggleDownloadsPanel() }) {
                            Icon(Icons.Default.Download, stringResource(R.string.catalog_downloads), tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Contenido principal - filtrado por tipo de fuente
            val hasLocalOrSmbContent = localFiles.isNotEmpty() || smbFiles.isNotEmpty()
            val isLoadingAny = uiState.isLoading || isLoadingExternalFiles
            
            when {
                isLoadingAny -> LoadingContent()
                uiState.error != null && showArchiveOrgContent -> ErrorContent(uiState.error!!) { viewModel.searchPacks() }
                else -> {
                    // Show combined content based on filter
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Archive.org packs (only if showing cloud content)
                        if (showArchiveOrgContent && uiState.filteredPacks.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.catalog_section_archive_org),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            items(uiState.filteredPacks) { pack ->
                                PackCard(
                                    pack = pack,
                                    isFullyDownloaded = pack.archiveIdentifier in uiState.fullyDownloadedPackIds,
                                    onClick = { viewModel.onPackSelected(pack) }
                                )
                            }
                            if (uiState.hasMorePages) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (uiState.isLoadingMore) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        } else {
                                            TextButton(onClick = { viewModel.loadMorePacks() }) {
                                                Text(stringResource(R.string.catalog_load_more))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // itch.io games
                        if (showItchContent) {
                            // Show header with API key button whenever itch.io content is visible and key is missing
                            if (selectedSourceType == SourceType.ITCH_IO || uiState.itchGames.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            stringResource(R.string.itch_source_name),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!uiState.itchApiKeyConfigured) {
                                            TextButton(onClick = { showItchApiKeyDialog = true }) {
                                                Icon(Icons.Default.Key, null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(stringResource(R.string.itch_api_key_title), style = MaterialTheme.typography.labelSmall)
                                            }
                                        } else {
                                            // Key configured: show small icon indicator + edit button
                                            TextButton(onClick = { showItchApiKeyDialog = true }) {
                                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(stringResource(R.string.itch_settings_edit_key), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }

                            when {
                                uiState.itchUnsupportedSystem && selectedSourceType == SourceType.ITCH_IO -> {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stringResource(R.string.itch_unsupported_system),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                                uiState.itchGames.isNotEmpty() -> {
                                    items(uiState.itchGames) { game ->
                                        ItchGameCard(
                                            game = game,
                                            isFullyDownloaded = game.id in uiState.fullyDownloadedPackIds,
                                            onClick = { viewModel.onItchGameSelected(game) }
                                        )
                                    }
                                }
                                selectedSourceType == SourceType.ITCH_IO && !uiState.isLoading -> {
                                    item {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                stringResource(R.string.itch_no_results),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Local files (only if showing local content)
                        if (showLocalContent && localFiles.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.catalog_section_local_files),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            items(localFiles) { file ->
                                LocalFileCard(
                                    file = file,
                                    onPlay = {
                                        // La ROM local ya está en el dispositivo, se puede lanzar directamente
                                        // Esto se integrará con el sistema de biblioteca existente
                                    }
                                )
                            }
                        }
                        
                        // SMB files (only if showing SMB content)
                        if (showSmbContent && smbFiles.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.catalog_section_smb_nas),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            // Filter SMB files by search query
                            val filteredSmbFiles = if (uiState.searchQuery.isNotEmpty()) {
                                smbFiles.filter { (_, file) -> 
                                    file.name.contains(uiState.searchQuery, ignoreCase = true)
                                }
                            } else {
                                smbFiles
                            }
                            items(filteredSmbFiles) { (source, file) ->
                                val isDownloading = file.path in smbDownloadsInProgress
                                val isDownloaded = file.path in smbDownloadedFiles // V8.6 ANR FIX: Removed blocking call isFileInRomsDir
                                
                                SmbFileCard(
                                    file = file,
                                    sourceName = source.name,
                                    isDownloading = isDownloading,
                                    isDownloaded = isDownloaded,
                                    onDownload = {
                                        if (!isDownloading) {
                                            smbDownloadsInProgress = smbDownloadsInProgress + file.path
                                            coroutineScope.launch {
                                                try {
                                                    romDownloader.downloadFromSmbSource(file, source)
                                                    smbDownloadedFiles = smbDownloadedFiles + file.path
                                                } catch (e: Exception) {
                                                    Log.e("CatalogScreen", "Download failed", e)
                                                } finally {
                                                    smbDownloadsInProgress = smbDownloadsInProgress - file.path
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // Empty state
                        if ((selectedSourceType == null && uiState.filteredPacks.isEmpty() && localFiles.isEmpty() && smbFiles.isEmpty() && uiState.itchGames.isEmpty()) ||
                            (selectedSourceType == SourceType.ARCHIVE_ORG && uiState.filteredPacks.isEmpty()) ||
                            (selectedSourceType == SourceType.LOCAL && localFiles.isEmpty()) ||
                            (selectedSourceType == SourceType.SMB && smbFiles.isEmpty()) ||
                            (selectedSourceType == SourceType.ITCH_IO && uiState.itchGames.isEmpty() && !uiState.itchUnsupportedSystem)) {
                            item {
                                EmptyContent()
                            }
                        }
                    }
                }
            }
        }
        
        // Panel de descargas (fullscreen overlay)
        if (uiState.showDownloadsPanel) {
            DownloadsPanel(
                downloads = downloads.values.toList(),
                onDismiss = { viewModel.toggleDownloadsPanel() },
                onCancelDownload = { viewModel.cancelDownload(it) },
                onClearCompleted = { viewModel.clearCompletedDownloads() }
            )
        }
    }
    
    // Dialog de detalles del paquete
    uiState.selectedPack?.let { pack ->
        PackDetailsDialog(
            pack = pack,
            files = uiState.downloadableFiles,
            isLoadingFiles = uiState.isLoadingFiles,
            isFileDownloaded = { fileName -> 
                val downloadId = "${pack.id}_${fileName}"
                val isCompletedInMemory = downloads[downloadId]?.status == RomDownloader.DownloadStatus.COMPLETED
                isCompletedInMemory || uiState.downloadedFiles.contains(fileName) // V8.6 ANR FIX: State check
            },
            isFileDownloading = { fileName ->
                val downloadId = "${pack.id}_${fileName}"
                val status = downloads[downloadId]?.status
                status == RomDownloader.DownloadStatus.PENDING || 
                status == RomDownloader.DownloadStatus.DOWNLOADING || 
                status == RomDownloader.DownloadStatus.PROCESSING
            },
            onDismiss = { viewModel.clearSelectedPack() },
            onDownloadFile = { file -> viewModel.startDownload(pack, file) },
            onDownloadAll = { viewModel.downloadAllFiles(pack, uiState.downloadableFiles) }
        )
    }
    
    // Add Source Dialog
    if (showAddSourceDialog) {
        AddSourceDialog(
            onDismiss = { showAddSourceDialog = false },
            onAddLocal = {
                folderPickerLauncher.launch(null)
                showAddSourceDialog = false
            },
            onAddSmb = { name, server, _, path, credentials ->
                val newSource = RomSource.smb(name, server, path, credentials)
                sourceManager.addSource(newSource)
                sources = sourceManager.getSources()
                shouldReloadLocalFiles = true
            }
        )
    }
    
    // Manage Sources Dialog
    if (showManageSourcesDialog) {
        ManageSourcesDialog(
            sources = sources.filter { it.type != SourceType.ARCHIVE_ORG && it.type != SourceType.ITCH_IO },
            onDismiss = { showManageSourcesDialog = false },
            onEdit = { updatedSource ->
                sourceManager.updateSource(updatedSource)
                sources = sourceManager.getSources()
                shouldReloadLocalFiles = true
            },
            onDelete = { source ->
                sourceManager.removeSource(source.id)
                sources = sourceManager.getSources()
                shouldReloadLocalFiles = true
            }
        )
    }

    // itch.io API Key Dialog
    if (showItchApiKeyDialog) {
        ItchIoApiKeyDialog(
            onDismiss = { showItchApiKeyDialog = false },
            onKeySaved = {
                viewModel.refreshItchApiKeyState()
            }
        )
    }

    // itch.io API Key Required Dialog
    if (showItchApiKeyRequiredDialog) {
        ItchIoApiKeyRequiredDialog(
            onDismiss = {
                showItchApiKeyRequiredDialog = false
                viewModel.clearSelectedItchGame()
            },
            onConfigure = {
                showItchApiKeyRequiredDialog = false
                showItchApiKeyDialog = true
            }
        )
    }

    // itch.io Game Details Dialog
    uiState.selectedItchGame?.let { game ->
        ItchGameDetailsDialog(
            game = game,
            uploads = uiState.itchUploads,
            isLoadingFiles = uiState.isLoadingFiles,
            apiKeyConfigured = uiState.itchApiKeyConfigured,
            downloads = downloads,
            onDismiss = { viewModel.clearSelectedItchGame() },
            onDownload = { upload -> viewModel.startItchDownload(game, upload) },
            onConfigureApiKey = {
                viewModel.clearSelectedItchGame()
                showItchApiKeyDialog = true
            }
        )
    }

    invalidCatalogDownload?.let { download ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDownload(download.id) },
            title = { Text(stringResource(R.string.catalog_invalid_download_title)) },
            text = {
                Column {
                    Text(download.fileName, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(download.error ?: stringResource(R.string.catalog_invalid_download_message))
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelDownload(download.id) }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // CRITICAL ERROR DIALOG: PATH NOT CONFIGURED
    if (uiState.error == "NO_ROM_PATH_CONFIGURED") {
        AlertDialog(
            onDismissRequest = { viewModel.searchPacks() },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.catalog_path_warning_title)) 
                }
            },
            text = { 
                Text(stringResource(R.string.catalog_path_select_folder)) 
            },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.searchPacks()
                        val intent = Intent(context, com.swordfish.lemuroid.app.shared.settings.StorageFrameworkPickerLauncher::class.java)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.catalog_path_select_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.searchPacks() }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // STORAGE ERROR DIALOG: NOT ENOUGH SPACE
    uiState.storageCheckResult?.let { storageCheck ->
        if (!storageCheck.hasEnoughSpace) {
            AlertDialog(
                onDismissRequest = { viewModel.clearStorageError() },
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.catalog_storage_warning_title)) 
                    }
                },
                text = { 
                    Column {
                        Text(
                            stringResource(R.string.catalog_storage_too_large),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.catalog_storage_download_size), modifier = Modifier.weight(1f))
                            Text(storageCheck.requiredFormatted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.catalog_storage_available), modifier = Modifier.weight(1f))
                            Text(storageCheck.availableFormatted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.catalog_storage_needed), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.error)
                            Text(storageCheck.shortageFormatted, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.catalog_storage_free_space),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearStorageError() }) {
                        Text(stringResource(R.string.catalog_storage_understood))
                    }
                }
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(error: String, onRetry: () -> Unit) {
    // User-friendly error messages
    val displayMessage = when {
        error.contains("503") -> stringResource(R.string.catalog_error_503)
        error.contains("502") -> stringResource(R.string.catalog_error_502)
        error.contains("500") -> stringResource(R.string.catalog_error_500)
        error.contains("certificate", ignoreCase = true) || error.contains("SSL", ignoreCase = true) -> 
            stringResource(R.string.catalog_error_ssl)
        error.contains("timeout", ignoreCase = true) -> stringResource(R.string.catalog_error_timeout)
        error.contains("Unable to resolve host", ignoreCase = true) -> stringResource(R.string.catalog_error_no_internet)
        else -> error
    }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(displayMessage, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.catalog_error_retry)) }
        }
    }
}

@Composable
private fun EmptyContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(R.string.catalog_no_packs_found), color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PacksList(
    packs: List<ArchiveOrgClient.RomPack>,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    onPackClick: (ArchiveOrgClient.RomPack) -> Unit,
    onLoadMore: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(packs, key = { index, pack -> "${pack.id}_$index" }) { _, pack ->
            PackCard(pack = pack, onClick = { onPackClick(pack) })
        }
        
        // Botón cargar más
        if (hasMore) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoadingMore) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    } else {
                        Button(onClick = onLoadMore) {
                            Icon(Icons.Default.ExpandMore, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.catalog_load_more_packs))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PackCard(
    pack: ArchiveOrgClient.RomPack,
    isFullyDownloaded: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pack.flag, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (isFullyDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.pack_fully_downloaded),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${pack.systemId.uppercase()} • ${pack.sizeFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "⬇️ ${pack.downloads}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            pack.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc.take(120) + if (desc.length > 120) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PackDetailsDialog(
    pack: ArchiveOrgClient.RomPack,
    files: List<ArchiveOrgClient.DownloadableFile>,
    isLoadingFiles: Boolean,
    isFileDownloaded: (String) -> Boolean,
    isFileDownloading: (String) -> Boolean,
    onDismiss: () -> Unit,
    onDownloadFile: (ArchiveOrgClient.DownloadableFile) -> Unit,
    onDownloadAll: () -> Unit
) {
    // Estado para búsqueda de archivos dentro del paquete
    var searchQuery by remember { mutableStateOf("") }
    
    // Filtrar archivos por búsqueda
    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(pack.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${pack.flag} ${pack.systemId.uppercase()} • ${pack.sizeFormatted} • ⬇️ ${pack.downloads}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Content (Search + List)
                Column(
                    modifier = Modifier
                        .height(500.dp) // Fixed height
                        .weight(1f, fill = false) // Allow shrinking if content is small, but cap at 500dp
                ) {
                    // Search bar
                    if (files.size > 10 && !isLoadingFiles) {
                         OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar ROM...", style = MaterialTheme.typography.bodySmall) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                    ) {
                        // Description
                        pack.description?.takeIf { it.isNotBlank() }?.let { desc ->
                            item {
                                Text(desc, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }

                        // Header
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val countText = if (searchQuery.isNotBlank() && filteredFiles.size != files.size) {
                                    "${filteredFiles.size} de ${files.size} archivos"
                                } else {
                                    stringResource(R.string.catalog_files_count, files.size)
                                }
                                Text(countText, style = MaterialTheme.typography.labelMedium)
                                if (files.isNotEmpty() && !isLoadingFiles) {
                                    FilledTonalButton(
                                        onClick = onDownloadAll,
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Icon(Icons.Default.Download, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.catalog_download_all), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // List content
                       when {
                            isLoadingFiles -> {
                                item {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            files.isEmpty() -> {
                                item {
                                    Text(stringResource(R.string.catalog_no_files_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            filteredFiles.isEmpty() -> {
                                item {
                                    Text("No se encontraron ROMs con \"$searchQuery\"", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            else -> {
                                itemsIndexed(filteredFiles, key = { index, file -> "${file.name}_$index" }) { _, file ->
                                    val downloaded = isFileDownloaded(file.name)
                                    val downloading = isFileDownloading(file.name)
                                    FileItem(
                                        file = file,
                                        isDownloaded = downloaded,
                                        isDownloading = downloading,
                                        onDownload = { onDownloadFile(file) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItem(
    file: ArchiveOrgClient.DownloadableFile,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (isDownloaded) "${file.sizeFormatted} • Ya descargado" else file.sizeFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = if (isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isDownloaded) {
            Icon(
                Icons.Default.CheckCircle,
                "Descargado",
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        } else if (isDownloading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.secondary
            )
        } else {
            IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Download, "Descargar", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
}

@Composable
private fun DownloadsPanel(
    downloads: List<RomDownloader.DownloadInfo>,
    onDismiss: () -> Unit,
    onCancelDownload: (String) -> Unit,
    onClearCompleted: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.catalog_downloads_title, downloads.size), style = MaterialTheme.typography.titleLarge)
                Row {
                    if (downloads.any { it.status in listOf(RomDownloader.DownloadStatus.COMPLETED, RomDownloader.DownloadStatus.ERROR) }) {
                        TextButton(onClick = onClearCompleted) { Text(stringResource(R.string.catalog_downloads_clear)) }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, stringResource(R.string.close))
                    }
                }
            }
            
            HorizontalDivider()
            
            if (downloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Download, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.catalog_no_downloads), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(downloads, key = { index, download -> "${download.id}_$index" }) { _, download ->
                        DownloadItem(download = download, onCancel = { onCancelDownload(download.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadItem(
    download: RomDownloader.DownloadInfo,
    onCancel: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.fileName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = download.gameTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                when (download.status) {
                    RomDownloader.DownloadStatus.PROCESSING,
                    RomDownloader.DownloadStatus.DOWNLOADING,
                    RomDownloader.DownloadStatus.PENDING -> {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Close, stringResource(R.string.cancel))
                        }
                    }
                    RomDownloader.DownloadStatus.COMPLETED -> {
                        Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    RomDownloader.DownloadStatus.ERROR -> {
                        Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                    }
                    RomDownloader.DownloadStatus.CANCELLED -> {
                        Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            
            if (download.status == RomDownloader.DownloadStatus.DOWNLOADING) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { download.progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(download.sizeText, style = MaterialTheme.typography.labelSmall)
                    Text(download.progressText, style = MaterialTheme.typography.labelSmall)
                }
            } else if (download.status == RomDownloader.DownloadStatus.ERROR) {
                Text(
                    text = download.error ?: "Error",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun LocalFileCard(
    file: LocalFile,
    onPlay: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag or system icon
            Text(
                text = file.flag,
                fontSize = 24.sp,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.cleanName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.system != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(file.systemDisplay, fontSize = 10.sp) },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                    Text(
                        text = "${file.sizeFormatted} • ${file.extension.uppercase()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SmbFileCard(
    file: SmbFile,
    sourceName: String,
    isDownloading: Boolean = false,
    isDownloaded: Boolean = false,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isDownloading, onClick = onDownload)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag or system icon
            Text(
                text = file.flag,
                fontSize = 24.sp,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.cleanName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (file.system != null) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(file.systemDisplay, fontSize = 10.sp) },
                            modifier = Modifier.height(20.dp)
                        )
                    }
                    Text(
                        text = "${file.sizeFormatted} • $sourceName",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Icon based on state
            when {
                isDownloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                isDownloaded -> {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "In Library",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                else -> {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun getRegionDisplayName(code: String): String {
    return when (code) {
        "" -> stringResource(R.string.catalog_region_all)
        "USA" -> stringResource(R.string.catalog_region_usa)
        "EUR" -> stringResource(R.string.catalog_region_europe)
        "JPN" -> stringResource(R.string.catalog_region_japan)
        "ESP" -> stringResource(R.string.catalog_region_spain)
        "FRA" -> stringResource(R.string.catalog_region_france)
        "GER" -> stringResource(R.string.catalog_region_germany)
        "ITA" -> stringResource(R.string.catalog_region_italy)
        "BRA" -> stringResource(R.string.catalog_region_brazil)
        "KOR" -> stringResource(R.string.catalog_region_korea)
        "CHN" -> stringResource(R.string.catalog_region_china)
        "AUS" -> stringResource(R.string.catalog_region_australia)
        else -> code
    }
}

@Composable
private fun getSortOptionDisplayName(option: CatalogViewModel.SortOption): String {
    return when (option) {
        CatalogViewModel.SortOption.DOWNLOADS -> stringResource(R.string.catalog_sort_downloads)
        CatalogViewModel.SortOption.NAME -> stringResource(R.string.catalog_sort_name)
        CatalogViewModel.SortOption.SIZE -> stringResource(R.string.catalog_sort_size)
    }
}

@Composable
private fun ItchGameCard(
    game: ItchGame,
    isFullyDownloaded: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cover image if available, otherwise placeholder flag
                if (!game.coverUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(game.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = game.plainTitle,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(game.flag, style = MaterialTheme.typography.titleLarge)
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.plainTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (game.author.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.itch_game_by, game.author),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (isFullyDownloaded) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.pack_fully_downloaded),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(game.systemId.uppercase(), fontSize = 10.sp) },
                        modifier = Modifier.height(20.dp)
                    )
                    Text(
                        text = game.sizeFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.itch_game_free),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            game.description?.takeIf { it.isNotBlank() }?.let { desc ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = desc.take(120) + if (desc.length > 120) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ItchGameDetailsDialog(
    game: ItchGame,
    uploads: List<ItchUpload>,
    isLoadingFiles: Boolean,
    apiKeyConfigured: Boolean,
    downloads: Map<String, RomDownloader.DownloadInfo>,
    onDismiss: () -> Unit,
    onDownload: (ItchUpload) -> Unit,
    onConfigureApiKey: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.extraLarge,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Cover image banner (if available)
                if (!game.coverUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(game.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = game.plainTitle,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                // Title
                Column(modifier = Modifier.padding(bottom = 16.dp)) {
                    Text(game.plainTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "${game.flag} ${game.systemId.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (game.author.isNotBlank()) {
                            Text(
                                stringResource(R.string.itch_game_by, game.author),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            stringResource(R.string.itch_game_free),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Description
                game.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // API Key warning if not configured
                if (!apiKeyConfigured) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.itch_api_key_required_message),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = onConfigureApiKey) {
                                Text(stringResource(R.string.ok))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Uploads section
                Text(
                    stringResource(R.string.itch_uploads_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoadingFiles -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                    uploads.isEmpty() && apiKeyConfigured -> {
                        Text(
                            stringResource(R.string.itch_no_rom_uploads),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(uploads) { upload ->
                                val downloadId = "itch_${game.id}_${upload.id}"
                                val downloadStatus = downloads[downloadId]?.status
                                val isDownloading = downloadStatus == RomDownloader.DownloadStatus.DOWNLOADING ||
                                    downloadStatus == RomDownloader.DownloadStatus.PENDING ||
                                    downloadStatus == RomDownloader.DownloadStatus.PROCESSING
                                val isCompleted = downloadStatus == RomDownloader.DownloadStatus.COMPLETED

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isCompleted) {
                                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = upload.displayName ?: upload.filename,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = upload.sizeFormatted,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    when {
                                        isCompleted -> Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        isDownloading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.secondary)
                                        apiKeyConfigured -> IconButton(onClick = { onDownload(upload) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Download, "Download", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                }

                // Close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

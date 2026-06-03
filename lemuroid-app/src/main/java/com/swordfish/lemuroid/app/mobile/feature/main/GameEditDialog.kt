package com.swordfish.lemuroid.app.mobile.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.swordfish.lemuroid.R
import com.swordfish.lemuroid.lib.library.SystemID
import com.swordfish.lemuroid.lib.library.db.entity.Game
import com.swordfish.lemuroid.lib.library.metadata.GameMetadata
import kotlinx.coroutines.launch

/**
 * Dialog for editing game metadata
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameEditDialog(
    game: Game,
    onSearchOnline: suspend (String) -> List<GameMetadata>,
    onDismiss: () -> Unit,
    onSave: (Game) -> Unit
) {
    var title by remember { mutableStateOf(game.title) }
    var systemId by remember { mutableStateOf(game.systemId) }
    var isUserLocked by remember { mutableStateOf(game.isUserLocked) }
    var coverFrontUrl by remember { mutableStateOf(game.coverFrontUrl) }
    var description by remember { mutableStateOf(game.description ?: "") }
    var year by remember { mutableStateOf(game.year) }
    var genre by remember { mutableStateOf(game.genre ?: "") }
    
    var expanded by remember { mutableStateOf(false) }
    var searchExpanded by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<GameMetadata>>(emptyList()) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Available systems from SystemID enum
    val availableSystems = remember {
        SystemID.values().map { sysId ->
            sysId.dbname to sysId.dbname.uppercase().replace("_", " ")
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.game_edit_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title with Search
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(stringResource(R.string.game_edit_field_title)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Search dropdown
                    Box {
                        FilledIconButton(
                            onClick = {
                                if (title.isNotBlank() && !isSearching) {
                                    isSearching = true
                                    searchExpanded = true
                                    coroutineScope.launch {
                                        searchResults = onSearchOnline(title)
                                        isSearching = false
                                    }
                                }
                            },
                            enabled = title.isNotBlank() && !isSearching
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.game_edit_search_online))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = searchExpanded,
                            onDismissRequest = { searchExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            if (isSearching) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.game_edit_searching)) },
                                    onClick = { }
                                )
                            } else if (searchResults.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.game_edit_no_results)) },
                                    onClick = { searchExpanded = false }
                                )
                            } else {
                                searchResults.forEach { result ->
                                    val resultText = buildString {
                                        append(result.name)
                                        if (result.year != null) append(" (${result.year})")
                                        if (!result.system.isNullOrBlank()) append(" - ${result.system}")
                                    }
                                    DropdownMenuItem(
                                        text = { Text(resultText) },
                                        onClick = {
                                            title = result.name ?: title
                                            if (!result.system.isNullOrBlank()) {
                                                val foundSystem = availableSystems.find { s -> s.first == result.system || s.second.equals(result.system, ignoreCase = true) }
                                                if (foundSystem != null) {
                                                    systemId = foundSystem.first
                                                }
                                            }
                                            description = result.description ?: description
                                            year = result.year ?: year
                                            genre = result.genre ?: genre
                                            if (!result.thumbnail.isNullOrBlank()) {
                                                coverFrontUrl = result.thumbnail
                                            }
                                            isUserLocked = true
                                            searchExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // System dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = availableSystems.find { it.first == systemId }?.second 
                            ?: systemId.uppercase().replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.game_edit_field_system)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableSystems.forEach { (sysDbName, sysDisplayName) ->
                            DropdownMenuItem(
                                text = { Text(sysDisplayName) },
                                onClick = {
                                    systemId = sysDbName
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))

                // Lock toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isUserLocked,
                        onCheckedChange = { isUserLocked = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.game_edit_unlock_metadata),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // File info (read-only)
                Text(
                    text = "${stringResource(R.string.game_edit_file_label)} ${game.fileName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updatedGame = game.copy(
                                title = title,
                                systemId = systemId,
                                coverFrontUrl = coverFrontUrl,
                                description = description.ifBlank { null },
                                year = year,
                                genre = genre.ifBlank { null },
                                isUserLocked = isUserLocked
                            )
                            onSave(updatedGame)
                            onDismiss()
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(stringResource(R.string.game_edit_save))
                    }
                }
            }
        }
    }
}

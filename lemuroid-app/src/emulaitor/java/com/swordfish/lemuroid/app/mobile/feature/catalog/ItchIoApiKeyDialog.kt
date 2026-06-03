package com.swordfish.lemuroid.app.mobile.feature.catalog

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.swordfish.lemuroid.R
import kotlinx.coroutines.launch

/**
 * Guided onboarding dialog for the itch.io API key.
 * Shows step-by-step instructions with clickable links,
 * an editable API key field, and live validation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItchIoApiKeyDialog(
    onDismiss: () -> Unit,
    onKeySaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyManager = remember { ItchIoKeyManager(context) }
    val client = remember { ItchIoClient() }

    var apiKey by remember { mutableStateOf(keyManager.getApiKey() ?: "") }
    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<Boolean?>(null) }
    var profileName by remember { mutableStateOf(keyManager.getProfileName()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text(stringResource(R.string.itch_api_key_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Subtitle
                Text(
                    text = stringResource(R.string.itch_api_key_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Step-by-step instructions with clickable links
                val instructionsText = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                        append("1. ")
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )) {
                            append("itch.io")
                        }
                        append(" — Create a free account\n")
                        append("2. ")
                        withStyle(SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline,
                            fontWeight = FontWeight.Medium
                        )) {
                            append("Settings → API Keys")
                        }
                        append(" — Generate a new key\n")
                        append("3. Copy the key and paste it below")
                    }
                }
                Text(
                    text = instructionsText,
                    style = MaterialTheme.typography.bodyMedium
                )

                // Quick-link buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://itch.io/"))
                            )
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("itch.io", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://itch.io/user/settings/api-keys"))
                            )
                        }
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("API Keys", style = MaterialTheme.typography.labelSmall)
                    }
                }

                HorizontalDivider()

                // API Key input field
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = {
                        apiKey = it.trim()
                        validationResult = null
                    },
                    label = { Text(stringResource(R.string.itch_api_key_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        when (validationResult) {
                            true -> Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                            false -> Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error)
                            null -> if (isValidating) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            }
                        }
                    },
                    supportingText = {
                        when (validationResult) {
                            true -> Text(
                                if (profileName != null) "${stringResource(R.string.itch_api_key_valid)} ($profileName)"
                                else stringResource(R.string.itch_api_key_valid),
                                color = MaterialTheme.colorScheme.primary
                            )
                            false -> Text(stringResource(R.string.itch_api_key_invalid), color = MaterialTheme.colorScheme.error)
                            null -> if (isValidating) Text(stringResource(R.string.itch_api_key_validating))
                        }
                    }
                )
            }
        },
        confirmButton = {
            // Save button: validates + saves + closes atomically
            Button(
                onClick = {
                    if (apiKey.isBlank()) return@Button
                    isValidating = true
                    validationResult = null
                    scope.launch {
                        val valid = client.validateApiKey(apiKey)
                        isValidating = false
                        validationResult = valid
                        if (valid) {
                            val name = client.getProfileName(apiKey)
                            profileName = name
                            keyManager.saveApiKey(apiKey)
                            keyManager.setKeyValid(true)
                            keyManager.saveProfileName(name)
                            onKeySaved()
                            onDismiss()
                        } else {
                            keyManager.setKeyValid(false)
                        }
                    }
                },
                enabled = apiKey.isNotBlank() && !isValidating
            ) {
                if (isValidating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Simple confirmation dialog that triggers the full API key onboarding dialog.
 * Shown when user tries to download from itch.io without a configured key.
 */
@Composable
fun ItchIoApiKeyRequiredDialog(
    onDismiss: () -> Unit,
    onConfigure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Key, contentDescription = null) },
        title = { Text(stringResource(R.string.itch_api_key_required_title)) },
        text = { Text(stringResource(R.string.itch_api_key_required_message)) },
        confirmButton = {
            Button(onClick = onConfigure) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

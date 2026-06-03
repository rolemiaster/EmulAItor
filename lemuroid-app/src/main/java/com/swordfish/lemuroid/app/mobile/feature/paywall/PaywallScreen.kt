package com.swordfish.lemuroid.app.mobile.feature.paywall

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.swordfish.lemuroid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel,
    onClose: () -> Unit,
    activity: Activity
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifetimePrice by viewModel.lifetimePrice.collectAsState()
    val subscriptionPrice by viewModel.subscriptionPrice.collectAsState()
    
    var gpaInput by remember { mutableStateOf("") }
    var showInstructions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.paywall_title)) },
                navigationIcon = {
                    // Could add a back button, but since they can't play, maybe just let them exit via hardware back
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.paywall_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            val subTextBase = stringResource(R.string.paywall_buy_subscription)
            val subPriceStr = subscriptionPrice ?: "1€"
            val subText = "$subTextBase ($subPriceStr)"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(100.dp)
                    .clickable { viewModel.buySubscription(activity) },
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.boton_pay),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = subText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            val lifeTextBase = stringResource(R.string.paywall_buy_lifetime)
            val lifePriceStr = lifetimePrice ?: "10€"
            val lifeText = "$lifeTextBase ($lifePriceStr)"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
                    .height(100.dp)
                    .clickable { viewModel.buyLifetime(activity) },
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.boton_pay),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Text(
                    text = lifeText,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Divider(modifier = Modifier.padding(bottom = 32.dp))

            Text(
                text = stringResource(R.string.paywall_restore_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.paywall_restore_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            OutlinedTextField(
                value = gpaInput,
                onValueChange = { gpaInput = it },
                label = { Text("GPA") },
                placeholder = { Text(stringResource(R.string.paywall_gpa_hint)) },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = { viewModel.restorePurchase(gpaInput) },
                enabled = gpaInput.isNotBlank() && uiState !is PaywallUiState.Loading,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                if (uiState is PaywallUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.paywall_restore_button))
                }
            }

            TextButton(
                onClick = { showInstructions = true },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(stringResource(R.string.paywall_where_is_receipt))
            }

            when (val state = uiState) {
                is PaywallUiState.Success -> {
                    Text(
                        text = stringResource(R.string.paywall_success),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    LaunchedEffect(Unit) {
                        onClose()
                    }
                }
                is PaywallUiState.Error -> {
                    val errorText = when (state.code) {
                        "invalid_gpa" -> stringResource(R.string.paywall_error_invalid_gpa)
                        "limit_exceeded" -> stringResource(R.string.paywall_error_limit_exceeded)
                        "network" -> stringResource(R.string.paywall_error_network)
                        else -> stringResource(R.string.paywall_error_unknown)
                    }
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                else -> {}
            }
        }

        if (showInstructions) {
            AlertDialog(
                onDismissRequest = { showInstructions = false },
                title = { Text(stringResource(R.string.paywall_receipt_instructions_title)) },
                text = {
                    Column {
                        Text(stringResource(R.string.paywall_receipt_instructions_1))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.paywall_receipt_instructions_2))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(R.string.paywall_support_fallback))
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pay.google.com"))
                            activity.startActivity(intent)
                        }
                    ) {
                        Text(stringResource(R.string.paywall_receipt_open_web))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showInstructions = false }) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            )
        }
    }
}

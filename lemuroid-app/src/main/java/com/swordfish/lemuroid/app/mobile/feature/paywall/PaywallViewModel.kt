package com.swordfish.lemuroid.app.mobile.feature.paywall

import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.swordfish.lemuroid.ext.feature.access.AccessManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.app.Activity

class PaywallViewModel(
    private val accessManager: AccessManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PaywallUiState>(PaywallUiState.Idle)
    val uiState: StateFlow<PaywallUiState> = _uiState

    val lifetimePrice: StateFlow<String?> = accessManager.billingManager.lifetimePrice
    val subscriptionPrice: StateFlow<String?> = accessManager.billingManager.subscriptionPrice

    private val client = OkHttpClient()

    fun restorePurchase(gpa: String) {
        if (gpa.isBlank()) return
        
        val normalizedGpa = normalizeGpa(gpa)
        
        _uiState.value = PaywallUiState.Loading
        
        viewModelScope.launch {
            val success = verifyGpa(normalizedGpa)
            if (success) {
                accessManager.grantLegacyAccess()
                _uiState.value = PaywallUiState.Success
            }
        }
    }

    private fun normalizeGpa(input: String): String {
        val numbersOnly = input.replace(Regex("[^0-9]"), "")
        if (numbersOnly.length == 17) {
            return "GPA.${numbersOnly.substring(0, 4)}-${numbersOnly.substring(4, 8)}-${numbersOnly.substring(8, 12)}-${numbersOnly.substring(12, 17)}"
        }
        // Return original if it doesn't match the 17-digit pattern so the server can reject it natively
        return input.trim()
    }

    fun buyLifetime(activity: Activity) {
        viewModelScope.launch {
            accessManager.billingManager.launchLifetimeBillingFlow(activity)
        }
    }

    fun buySubscription(activity: Activity) {
        viewModelScope.launch {
            accessManager.billingManager.launchSubscriptionBillingFlow(activity)
        }
    }

    private suspend fun verifyGpa(gpa: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            val json = JSONObject().apply {
                put("action", "validate_gpa")
                put("gpa", gpa)
                put("deviceId", deviceId)
            }
            
            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(SCRIPT_URL)
                .post(requestBody)
                .build()
                
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseString = response.body?.string()
                if (responseString != null) {
                    val responseJson = JSONObject(responseString)
                    if (responseJson.optBoolean("success", false)) {
                        return@withContext true
                    } else {
                        val errorCode = responseJson.optString("errorCode", "")
                        val mappedCode = when (errorCode) {
                            "ERROR_RECEIPT_NOT_FOUND" -> "invalid_gpa"
                            "ERROR_LIMIT_EXCEEDED" -> "limit_exceeded"
                            else -> "unknown"
                        }
                        _uiState.value = PaywallUiState.Error(mappedCode)
                        return@withContext false
                    }
                }
            }
            _uiState.value = PaywallUiState.Error("network")
            return@withContext false
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            _uiState.value = PaywallUiState.Error("network")
            return@withContext false
        } catch (e: Exception) {
            e.printStackTrace()
            _uiState.value = PaywallUiState.Error("unknown")
            return@withContext false
        }
    }

    class Factory(
        private val accessManager: AccessManager,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return PaywallViewModel(accessManager, context) as T
        }
    }

    companion object {
        private const val SCRIPT_URL = "https://script.google.com/macros/s/AKfycbxgLtUJcImGpEz0TdPtZZ852DjxdwxzsJ0GT1CjsMHdqErJ-BrNDh1O-RLjHmU5oyhhNg/exec"
    }
}

sealed class PaywallUiState {
    object Idle : PaywallUiState()
    object Loading : PaywallUiState()
    object Success : PaywallUiState()
    data class Error(val code: String) : PaywallUiState()
}

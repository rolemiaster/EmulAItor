package com.swordfish.lemuroid.app.mobile.feature.catalog

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log

/**
 * ItchIoKeyManager - Stores and retrieves the itch.io API key.
 *
 * Uses SharedPreferences with simple XOR obfuscation since
 * EncryptedSharedPreferences (security-crypto) is not a project dependency.
 * This is NOT cryptographically secure but prevents casual plaintext reading.
 */
class ItchIoKeyManager(context: Context) {

    companion object {
        private const val TAG = "ItchIoKeyManager"
        private const val PREFS_NAME = "itch_io_prefs"
        private const val KEY_API_KEY = "api_key_obfuscated"
        private const val KEY_API_KEY_VALID = "api_key_valid"
        private const val KEY_PROFILE_NAME = "profile_name"

        // Simple XOR mask for obfuscation (not crypto-grade)
        private const val XOR_MASK = 0x5A
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Save the API key with XOR obfuscation.
     */
    fun saveApiKey(apiKey: String) {
        val obfuscated = xorObfuscateBytes(apiKey.toByteArray())
        val encoded = Base64.encodeToString(obfuscated, Base64.NO_WRAP)
        // Use commit() synchronously so subsequent hasApiKey() reads are consistent
        prefs.edit()
            .putString(KEY_API_KEY, encoded)
            .putBoolean(KEY_API_KEY_VALID, true)
            .commit()
        Log.d(TAG, "API key saved (${apiKey.length} chars)")
    }

    /**
     * Retrieve the API key (de-obfuscated).
     * Returns null if no key is stored.
     */
    fun getApiKey(): String? {
        val encoded = prefs.getString(KEY_API_KEY, null) ?: return null
        return try {
            val obfuscated = Base64.decode(encoded, Base64.NO_WRAP)
            val deobfuscated = xorObfuscateBytes(obfuscated)
            String(deobfuscated)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode API key: ${e.message}")
            null
        }
    }

    /**
     * Check if an API key has been configured.
     */
    fun hasApiKey(): Boolean = prefs.getString(KEY_API_KEY, null) != null

    /**
     * Mark the API key as validated (confirmed working with itch.io).
     */
    fun setKeyValid(valid: Boolean) {
        prefs.edit().putBoolean(KEY_API_KEY_VALID, valid).apply()
    }

    /**
     * Check if the API key has been validated.
     */
    fun isKeyValid(): Boolean = prefs.getBoolean(KEY_API_KEY_VALID, false)

    /**
     * Save the profile name associated with the API key.
     */
    fun saveProfileName(name: String?) {
        prefs.edit().putString(KEY_PROFILE_NAME, name).apply()
    }

    /**
     * Get the profile name associated with the API key.
     */
    fun getProfileName(): String? = prefs.getString(KEY_PROFILE_NAME, null)

    /**
     * Remove the API key entirely.
     */
    fun clearApiKey() {
        prefs.edit().clear().apply()
        Log.d(TAG, "API key cleared")
    }

    /**
     * Simple XOR obfuscation/de-obfuscation.
     * Same operation works both ways since XOR is its own inverse.
     */
    private fun xorObfuscateBytes(input: ByteArray): ByteArray {
        val output = input.copyOf()
        for (i in output.indices) {
            output[i] = (output[i].toInt() xor XOR_MASK).toByte()
        }
        return output
    }
}

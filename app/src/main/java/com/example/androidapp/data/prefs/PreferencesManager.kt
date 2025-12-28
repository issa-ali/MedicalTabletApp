package com.example.androidapp.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class PreferencesManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveBaseUrl(ip: String, port: String) {
        sharedPreferences.edit()
            .putString("backend_ip", ip)
            .putString("backend_port", port)
            .apply()
    }
    
    fun getBaseUrl(): String {
        // Default as per product vision
        val ip = sharedPreferences.getString("backend_ip", "192.168.1.102") ?: "192.168.1.102"
        val port = sharedPreferences.getString("backend_port", "3000") ?: "3000"
        return "http://$ip:$port/api/"
    }

    fun isReset(): Boolean {
        // Helper to check if we're using defaults or custom
        return !sharedPreferences.contains("backend_ip")
    }
}

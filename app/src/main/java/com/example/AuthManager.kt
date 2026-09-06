package com.example

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

data class UserSession(
    val id: String,
    val name: String,
    val email: String,
    val profilePictureUri: String? = null,
    val authType: String = "email" // email, google, phone
)

class AuthManager(private val context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "clipp_auth_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        val userJson = sharedPreferences.getString("current_user", null)
        if (userJson != null) {
            try {
                val json = JSONObject(userJson)
                _currentUser.value = UserSession(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    email = json.getString("email"),
                    profilePictureUri = if (json.has("profilePictureUri")) json.getString("profilePictureUri") else null,
                    authType = json.getString("authType")
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveSession(user: UserSession, token: String) {
        val json = JSONObject().apply {
            put("id", user.id)
            put("name", user.name)
            put("email", user.email)
            if (user.profilePictureUri != null) put("profilePictureUri", user.profilePictureUri)
            put("authType", user.authType)
        }
        sharedPreferences.edit()
            .putString("current_user", json.toString())
            .putString("auth_token", token) // secure token storage
            .apply()
        _currentUser.value = user
    }

    fun logout() {
        sharedPreferences.edit().clear().apply()
        _currentUser.value = null
    }

    fun deleteAccount() {
        // In a real app this would call an API to delete the account
        logout()
    }
}

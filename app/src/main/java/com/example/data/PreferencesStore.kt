package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppLang
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gtDataStore by preferencesDataStore(name = "growthtwin_prefs")

const val DEFAULT_SERVER_URL = "https://growth-twin.onrender.com"

data class UserPrefs(
    val darkTheme: Boolean = true,
    val lang: AppLang = AppLang.BN,
    val isLoggedIn: Boolean = false,
    val userId: String = "farmer1",
    val userName: String = "farmer1",
    val userEmail: String = "",
    val userRole: String = "farmer", // farmer | expert | admin
    val authToken: String = "",
    val authProvider: String = "dev", // server | supabase | local | dev
    val serverBaseUrl: String = DEFAULT_SERVER_URL,
    val checklistDone: Set<String> = emptySet(),
    val whatsappDemoLinked: Boolean = false,
    val whatsappPhone: String = "",
    val whatsappLinkCode: String = ""
)

class PreferencesStore(private val context: Context) {
    private val keyDark = booleanPreferencesKey("dark_theme")
    private val keyLang = stringPreferencesKey("app_lang")
    private val keyLoggedIn = booleanPreferencesKey("is_logged_in")
    private val keyUserId = stringPreferencesKey("user_id")
    private val keyUserName = stringPreferencesKey("user_name")
    private val keyUserEmail = stringPreferencesKey("user_email")
    private val keyRole = stringPreferencesKey("user_role")
    private val keyAuthToken = stringPreferencesKey("auth_token")
    private val keyAuthProvider = stringPreferencesKey("auth_provider")
    private val keyServerUrl = stringPreferencesKey("server_base_url")
    private val keyChecklist = stringPreferencesKey("sentinel_checklist")
    private val keyWaLinked = booleanPreferencesKey("wa_linked")
    private val keyWaPhone = stringPreferencesKey("wa_phone")
    private val keyWaCode = stringPreferencesKey("wa_code")

    val prefsFlow: Flow<UserPrefs> = context.gtDataStore.data.map { p ->
        val rawChk = p[keyChecklist] ?: ""
        UserPrefs(
            darkTheme = p[keyDark] ?: true,
            lang = AppLang.fromCode(p[keyLang]),
            isLoggedIn = p[keyLoggedIn] ?: false,
            userId = p[keyUserId] ?: "farmer1",
            userName = p[keyUserName] ?: (p[keyUserId] ?: "farmer1"),
            userEmail = p[keyUserEmail] ?: "",
            userRole = p[keyRole] ?: "farmer",
            authToken = p[keyAuthToken] ?: "",
            authProvider = p[keyAuthProvider] ?: "dev",
            serverBaseUrl = p[keyServerUrl] ?: DEFAULT_SERVER_URL,
            checklistDone = rawChk.split(",").filter { it.isNotBlank() }.toSet(),
            whatsappDemoLinked = p[keyWaLinked] ?: false,
            whatsappPhone = p[keyWaPhone] ?: "",
            whatsappLinkCode = p[keyWaCode] ?: ""
        )
    }

    suspend fun setDarkTheme(dark: Boolean) {
        context.gtDataStore.edit { it[keyDark] = dark }
    }

    suspend fun setLang(lang: AppLang) {
        context.gtDataStore.edit { it[keyLang] = lang.code }
    }

    suspend fun setIdentity(userId: String, role: String) {
        context.gtDataStore.edit {
            it[keyUserId] = userId.ifBlank { "farmer1" }
            it[keyUserName] = userId.ifBlank { "farmer1" }
            it[keyRole] = role
            it[keyLoggedIn] = true
            it[keyAuthProvider] = "dev"
        }
    }

    suspend fun saveAuthSession(session: AuthSessionResult) {
        context.gtDataStore.edit {
            it[keyLoggedIn] = true
            it[keyUserId] = session.userId.ifBlank { "farmer1" }
            it[keyUserName] = session.name.ifBlank { session.userId }
            it[keyUserEmail] = session.email
            it[keyRole] = session.role
            it[keyAuthToken] = session.accessToken
            it[keyAuthProvider] = session.provider
        }
    }

    suspend fun logout() {
        context.gtDataStore.edit {
            it[keyLoggedIn] = false
            it[keyAuthToken] = ""
            it[keyUserEmail] = ""
        }
    }

    suspend fun setServerBaseUrl(url: String) {
        context.gtDataStore.edit { it[keyServerUrl] = url.trim().trimEnd('/') }
    }

    suspend fun toggleChecklistItem(id: String) {
        context.gtDataStore.edit { p ->
            val cur = (p[keyChecklist] ?: "").split(",").filter { it.isNotBlank() }.toMutableSet()
            if (id in cur) cur.remove(id) else cur.add(id)
            p[keyChecklist] = cur.joinToString(",")
        }
    }

    suspend fun resetChecklist() {
        context.gtDataStore.edit { it[keyChecklist] = "" }
    }

    suspend fun setWhatsappLinkCode(code: String) {
        context.gtDataStore.edit { it[keyWaCode] = code }
    }

    suspend fun setWhatsappDemoLinked(linked: Boolean, phone: String) {
        context.gtDataStore.edit {
            it[keyWaLinked] = linked
            it[keyWaPhone] = phone
        }
    }
}

package ru.faserkraft.client.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuthImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
) : AppAuth {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    override fun saveSession(token: String, expiresAt: String, userData: UserData) {
        prefs.edit {
            clear()
            putString(MOBILE_TOKEN, token)
            putString(EXPIRES_AT, expiresAt)
            putString(EMAIL, userData.email)
            putString(USERNAME, userData.name)
            userData.role?.value?.let { putString(ROLE, it) }
        }
    }

    override fun getRegistrationData(): UserData? {
        if (getToken().isNullOrBlank()) return null
        val email = prefs.getString(EMAIL, null) ?: return null
        val name = prefs.getString(USERNAME, null) ?: "Не зарегистрирован"
        val role = prefs.getString(ROLE, null)?.let(UserRole::fromValue)
        return UserData(email = email, name = name, role = role)
    }

    override fun getToken(): String? = prefs.getString(MOBILE_TOKEN, null)

    override fun checkRegistration(): String? =
        if (getToken().isNullOrBlank()) null else prefs.getString(EMAIL, null)

    override fun clear() {
        prefs.edit { clear() }
    }

    private companion object {
        const val MOBILE_TOKEN = "MOBILE_TOKEN"
        const val EXPIRES_AT = "EXPIRES_AT"
        const val EMAIL = "EMAIL"
        const val USERNAME = "USERNAME"
        const val ROLE = "ROLE"
    }
}
package ru.faserkraft.client.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.dto.LoginData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuthImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
) : AppAuth {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    override fun saveUserData(userData: UserData) {
        prefs.edit {
            putString(LOGIN, userData.email)
            putString(PASSWORD, userData.password)
            putString(USERNAME, userData.name)
            userData.role?.value?.let { putString(ROLE, it) } ?: remove(ROLE)
        }
    }

    override fun getRegistrationData(): UserData? {
        val email = prefs.getString(LOGIN, null) ?: return null
        val name = prefs.getString(USERNAME, "Не зарегистрирован") ?: "Не зарегистрирован"
        val password = prefs.getString(PASSWORD, "") ?: ""
        val roleString = prefs.getString(ROLE, null)

        val role = roleString?.let(UserRole::fromValue)

        return UserData(
            email = email,
            password = password,
            name = name,
            role = role,
        )
    }

    override fun getLoginData(): LoginData? {
        val login = prefs.getString(LOGIN, null)
        val password = prefs.getString(PASSWORD, null)

        return if (!login.isNullOrBlank() && !password.isNullOrBlank()) {
            LoginData(username = login, password = password)
        } else {
            null
        }
    }

    override fun saveToken(token: String) {
        prefs.edit {
            putString(TOKEN, token)
        }
    }

    override fun getToken(): String? =
        prefs.getString(TOKEN, null)

    override fun checkRegistration(): String? =
        prefs.getString(LOGIN, null)

    override fun clear() {
        prefs.edit {
            remove(LOGIN)
            remove(PASSWORD)
            remove(USERNAME)
            remove(ROLE)
            remove(TOKEN)
        }
    }

    private companion object {
        const val LOGIN = "LOGIN"
        const val PASSWORD = "PASSWORD"
        const val USERNAME = "USERNAME"
        const val ROLE = "ROLE"
        const val TOKEN = "TOKEN"
    }
}
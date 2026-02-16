package ru.faserkraft.client.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.model.UserData
import ru.faserkraft.client.model.UserRole
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuthImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
) : AppAuth {

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    companion object {
        private const val LOGIN = "LOGIN"
        private const val PASSWORD = "PASSWORD"
        private const val USERNAME = "USERNAME"
        private const val ROLE = "ROLE"
        private const val TOKEN = "TOKEN"
    }

    override fun saveUserData(
        userData: UserData
    ) {
        prefs.edit {
            putString(LOGIN, userData.email)
            putString(PASSWORD, userData.password)
            putString(USERNAME, userData.name)
            putString(ROLE, userData.role?.value.toString())
        }
    }

    override fun getRegistrationData(): UserData {
        val name = prefs.getString(USERNAME, "") ?: "Не зарегистрирован"
        val email = prefs.getString(LOGIN, "") ?: ""
        val roleString = prefs.getString(ROLE, null)

        val role = roleString?.let { value ->
            UserRole.entries.firstOrNull { it.value == value }
        }

        return UserData(
            email = email,
            password = "",
            name = name,
            role = role,
        )
    }

    override fun resetRegistration() {
        prefs.edit {
            putString(LOGIN, null)
            putString(PASSWORD, null)
            putString(USERNAME, "Не зарегистрирован")
            putString(TOKEN, null)
        }
    }

    override fun getLoginData(): LoginData? {
        val login = prefs.getString(LOGIN, null)
        val password = prefs.getString(PASSWORD, null)
        return if (login != null && password != null) {
            LoginData(username = login, password = password)
        } else null
    }

    override fun saveToken(token: String) {
        prefs.edit {
            putString(TOKEN, token)
        }
    }

    override fun getToken() = prefs.getString(TOKEN, null)

    override fun checkRegistration(): String? {
        return prefs.getString(LOGIN, null)
    }
}
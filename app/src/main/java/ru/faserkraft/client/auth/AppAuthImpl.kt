package ru.faserkraft.client.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import ru.faserkraft.client.dto.LoginData
import ru.faserkraft.client.model.RegistrationModel
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
        private const val TOKEN = "TOKEN"
    }

    override fun saveUserData(
        email: String,
        password: String,
        userName: String,
    ) {
        prefs.edit {
            putString(LOGIN, email)
            putString(PASSWORD, password)
            putString(USERNAME, userName)
        }
    }

    override fun getRegistrationData(): RegistrationModel {
        val userName = prefs.getString(USERNAME, "") ?: "Не зарегистрирован"
        val email = prefs.getString(LOGIN, "") ?: ""
        return RegistrationModel(
            employeeName = userName,
            email = email,
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
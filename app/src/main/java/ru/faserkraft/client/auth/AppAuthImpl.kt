package ru.faserkraft.client.auth

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppAuthImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
) : AppAuth{

    private val prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE)

    companion object {
        private const val LOGIN = "LOGIN"
        private const val PASSWORD = "PASSWORD"
        private const val TOKEN = "TOKEN"
    }

    override fun setLoginData(email: String, password: String) {
        prefs.edit {
            putString(LOGIN, email)
            putString(PASSWORD, password)
        }
    }

    override fun saveToken(token: String) {
        prefs.edit {
            putString(TOKEN, token)
        }
    }

    override fun getToken() = prefs.getString(TOKEN, null)
}
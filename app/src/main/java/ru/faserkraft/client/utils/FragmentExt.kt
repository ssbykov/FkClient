package ru.faserkraft.client.utils

import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar

fun Fragment.showErrorSnackbar(message: String) {
    val view = view ?: return
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
}
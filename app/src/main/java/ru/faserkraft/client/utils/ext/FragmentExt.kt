package ru.faserkraft.client.utils.ext

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar


fun Fragment.showErrorSnackbar(message: String) {
    val view = view ?: return
    Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
}


/**
 * Скрывает системную клавиатуру, если она открыта.
 * Безопасен к вызову в любой момент жизненного цикла фрагмента:
 * если view уже отсутствует (например, после onDestroyView), просто ничего не делает.
 */
fun Fragment.hideKeyboard() {
    val currentView = view ?: return
    val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
    imm?.hideSoftInputFromWindow(currentView.windowToken, 0)
}

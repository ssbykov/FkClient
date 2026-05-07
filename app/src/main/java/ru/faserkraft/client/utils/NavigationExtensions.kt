package ru.faserkraft.client.utils

import androidx.annotation.IdRes
import androidx.navigation.NavController

fun NavController.navigateSafely(@IdRes actionId: Int) {
    val currentDestinationId = currentDestination?.id
    val action = currentDestination?.getAction(actionId)

    // Если экшен существует для текущего экрана, безопасно переходим
    if (action != null && currentDestinationId != action.destinationId) {
        navigate(actionId)
    }
}
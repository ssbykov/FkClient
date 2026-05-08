package ru.faserkraft.client.utils

import androidx.navigation.NavController
import androidx.navigation.NavDirections

fun NavController.navigateSafely(actionId: Int) {
    val currentDestination = this.currentDestination
    val action = currentDestination?.getAction(actionId)
    if (action != null) {
        this.navigate(actionId)
    }
}

fun NavController.navigateSafely(directions: NavDirections) {
    val currentDestination = this.currentDestination
    val action = currentDestination?.getAction(directions.actionId)
    if (action != null) {
        this.navigate(directions)
    }
}
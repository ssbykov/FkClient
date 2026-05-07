package ru.faserkraft.client.presentation.product

import ru.faserkraft.client.domain.model.Step

sealed interface ProductEvent {
    object NavigateToNewProduct : ProductEvent
    object NavigateToProduct : ProductEvent
    data class NavigateToEditProcess(val productId: Long) : ProductEvent
    data class NavigateToEditStatus(val productId: Long) : ProductEvent
    data class ShowError(val message: String) : ProductEvent
    data class ShowConfirmationDialog(
        val title: String,
        val message: String,
        val actionType: ConfirmationActionType,
        val step: Step? = null,
    ) : ProductEvent
}

enum class ConfirmationActionType {
    CHANGE_STATUS,
    CHANGE_PROCESS,
    CLOSE_STEP,
}
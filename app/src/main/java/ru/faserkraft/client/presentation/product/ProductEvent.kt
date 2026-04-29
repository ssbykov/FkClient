package ru.faserkraft.client.presentation.product

sealed interface ProductEvent {
    data object NavigateToProduct : ProductEvent
    data object NavigateToNewProduct : ProductEvent
    data class ShowError(val message: String) : ProductEvent
}
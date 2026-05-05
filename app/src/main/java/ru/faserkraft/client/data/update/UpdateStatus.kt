package ru.faserkraft.client.data.update

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Pending : UpdateStatus
    data class Downloading(val percent: Int) : UpdateStatus
    data object Installing : UpdateStatus
    data class Error(val message: String) : UpdateStatus
}
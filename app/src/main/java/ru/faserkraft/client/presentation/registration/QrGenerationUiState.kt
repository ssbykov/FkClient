package ru.faserkraft.client.presentation.registration

import android.graphics.Bitmap
import ru.faserkraft.client.domain.model.Employee

data class QrGenerationUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val employees: List<Employee> = emptyList(),
    val qrBitmap: Bitmap? = null,
)
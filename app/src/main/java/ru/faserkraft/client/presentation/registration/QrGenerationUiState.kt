import ru.faserkraft.client.domain.model.Employee

data class QrGenerationUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val employees: List<Employee> = emptyList(),
    val qrBitmap: android.graphics.Bitmap? = null,
)
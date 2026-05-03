sealed class QrGenerationEvent {
    data class ShowError(val message: String) : QrGenerationEvent()
}
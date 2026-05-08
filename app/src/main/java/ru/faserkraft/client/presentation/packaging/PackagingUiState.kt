package ru.faserkraft.client.presentation.packaging

import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PackagingUiState(
    val currentPackaging: Packaging? = null,
    val packagingInStorage: List<Packaging> = emptyList(),
    val availableProducts: List<FinishedProduct> = emptyList(),
    val currentUser: UserData? = null,
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
) {
    val canEdit: Boolean
        get() {
            val role = currentUser?.role
            val userEmail = currentUser?.email
            val packagingEmail = currentPackaging?.performedBy?.email
            val orderId = currentPackaging?.orderId

            val isCreatedToday = currentPackaging?.performedAt?.let { dateStr ->
                isDateToday(dateStr)
            } ?: false

            return orderId == null && (
                    role == UserRole.ADMIN ||
                            role == UserRole.MASTER ||
                            (packagingEmail == userEmail && isCreatedToday)
                    )
        }

    private fun isDateToday(isoDateString: String): Boolean {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date: Date = parser.parse(isoDateString) ?: return false
            val today = Calendar.getInstance()
            val targetDate = Calendar.getInstance().apply {
                time = date
            }

            today.get(Calendar.YEAR) == targetDate.get(Calendar.YEAR) &&
                    today.get(Calendar.DAY_OF_YEAR) == targetDate.get(Calendar.DAY_OF_YEAR)

        } catch (e: Exception) {
            false
        }
    }
}
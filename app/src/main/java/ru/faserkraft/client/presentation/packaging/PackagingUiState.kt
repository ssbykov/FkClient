package ru.faserkraft.client.presentation.packaging

import android.os.Build
import androidx.annotation.RequiresApi
import ru.faserkraft.client.domain.model.FinishedProduct
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PackagingUiState(
    val currentPackaging: Packaging? = null,
    val packagingInStorage: List<Packaging> = emptyList(),
    val availableProducts: List<FinishedProduct> = emptyList(),
    val currentUser: UserData? = null,
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
) {
    val canEdit: Boolean
        @RequiresApi(Build.VERSION_CODES.O)
        get() {
            val role = currentUser?.role
            val userEmail = currentUser?.email
            val packagingEmail = currentPackaging?.performedBy?.email
            val orderId = currentPackaging?.orderId

            val isCreatedToday = runCatching {
                currentPackaging?.performedAt?.let {
                    Instant.parse(it)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == LocalDate.now()
                } ?: false
            }.getOrDefault(false)

            return orderId == null && (
                    role == UserRole.ADMIN ||
                            role == UserRole.MASTER ||
                            (packagingEmail == userEmail && isCreatedToday)
                    )
        }
}

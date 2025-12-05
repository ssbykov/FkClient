package ru.faserkraft.client.dto

import androidx.annotation.DrawableRes
import com.google.gson.annotations.SerializedName
import ru.faserkraft.client.R

data class StepDto(
    val id: Int,
    @SerializedName("product_id")
    val productId: Int,
    @SerializedName("step_definition")
    val stepDefinition: StepDefinitionDto,
    val status: String,
    @SerializedName("performed_by_id")
    val performedById: Int?,
    @SerializedName("performed_by")
    val performedBy: EmployeeDto?,
    @SerializedName("performed_at")
    val performedAt: String?
): ItemDto()

data class StepCloseDto(
    val id: Int,
    @SerializedName("performed_by")
    val performedBy: String
): ItemDto()

enum class StepStatusUi(
    val title: String,
    val description: String,
    @param:DrawableRes
    val iconRes: Int
) {
    DONE(
        title = "Статус: Выполнено",
        description = "Этап закрыт",
        iconRes = R.drawable.status_done
    ),
    WAITING(
        title = "Статус: Ожидает начала",
        description = "Пока ничего не сделано",
        iconRes = R.drawable.status_pending
    )
}

fun StepDto.toUiStatus(): StepStatusUi = when {
    status == "done" -> StepStatusUi.DONE
    else -> StepStatusUi.WAITING
}


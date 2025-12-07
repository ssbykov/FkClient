package ru.faserkraft.client.dto

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
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
    @StringRes
    val statusTitleRes: Int,
    @StringRes
    val statusDescRes: Int,
    @param:DrawableRes
    val iconRes: Int,
    @ColorRes
    val bgColorRes: Int
) {
    DONE(
        statusTitleRes = R.string.step_last_status_done,
        statusDescRes = R.string.step_last_desc_done,
        iconRes = R.drawable.status_done,
        bgColorRes = R.color.step_done_bg
    ),
    PENDING(
        statusTitleRes = R.string.step_last_status_pending,
        statusDescRes = R.string.step_last_desc_pending,
        iconRes = R.drawable.status_pending,
        bgColorRes = R.color.step_pending_bg
    )
}

enum class StepStatusBackend(val raw: String) {
    DONE("done"),
    PENDING("pending")
}

fun StepDto.toUiStatus(): StepStatusUi =
    when (status.lowercase()) {
        StepStatusBackend.DONE.raw -> StepStatusUi.DONE
        StepStatusBackend.PENDING.raw -> StepStatusUi.PENDING
        else -> StepStatusUi.PENDING
    }

val emptyStep = StepDto(
    id = 0,
    productId = 0,
    stepDefinition = StepDefinitionDto(0, 0, TemplateDto("")),
    status = "",
    performedById = null,
    performedBy = null,
    performedAt = null
)

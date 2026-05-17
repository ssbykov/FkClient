package ru.faserkraft.client.presentation.product.detail

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.faserkraft.client.R
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepStatus

// ---------- UI-модели ----------

data class StepStatusUi(
    @StringRes val statusTitleRes: Int,
    @StringRes val statusDescRes: Int,
    @DrawableRes val iconRes: Int,
    @ColorRes val bgColorRes: Int,
)

data class ProductStatusUi(
    @StringRes val titleRes: Int,
    @ColorRes val bgColorRes: Int,
    @ColorRes val textColorRes: Int,
    val originalStatus: ProductStatus
) {
    /**
     * Вся логика выбора текста инкапсулирована здесь.
     * View не знает про условия REWORK, SCRAP и т.д.
     */
    fun getTitle(context: Context): String {
        return when (originalStatus) {
            ProductStatus.REWORK, ProductStatus.SCRAP -> context.getString(titleRes)
            else -> originalStatus.name
        }
    }
}

// ---------- Маппинг Step → StepStatusUi ----------

fun Step.toUiStatus(): StepStatusUi = when (status) {
    StepStatus.DONE -> StepStatusUi(
        statusTitleRes = R.string.step_last_status_done,
        statusDescRes = R.string.step_last_desc_done,
        iconRes = R.drawable.status_done,
        bgColorRes = R.color.step_done_bg,
    )

    StepStatus.PENDING -> StepStatusUi(
        statusTitleRes = R.string.step_last_status_pending,
        statusDescRes = R.string.step_last_desc_pending,
        iconRes = R.drawable.status_pending,
        bgColorRes = R.color.step_pending_bg,
    )
}

// ---------- Маппинг ProductStatus → ProductStatusUi ----------

fun ProductStatus.toUiProductStatus(): ProductStatusUi = when (this) {
    ProductStatus.NORMAL -> ProductStatusUi(
        titleRes = R.string.normal,
        bgColorRes = R.color.status_success_bg,
        textColorRes = R.color.status_success_text,
        originalStatus = this
    )

    ProductStatus.REWORK -> ProductStatusUi(
        titleRes = R.string.rework,
        bgColorRes = R.color.status_rework_bg,
        textColorRes = R.color.status_rework_text,
        originalStatus = this
    )

    ProductStatus.SCRAP -> ProductStatusUi(
        titleRes = R.string.scrap,
        bgColorRes = R.color.status_scrap_bg,
        textColorRes = R.color.status_scrap_text,
        originalStatus = this
    )
}
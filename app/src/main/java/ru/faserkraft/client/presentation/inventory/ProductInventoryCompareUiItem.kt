package ru.faserkraft.client.presentation.inventory

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import ru.faserkraft.client.R
import ru.faserkraft.client.domain.model.ProductInventoryCompareItem
import ru.faserkraft.client.domain.model.StepDefinitionWithProcess

enum class CompareStatus(
    @StringRes val titleRes: Int,
    @ColorRes val colorRes: Int,
    @DrawableRes val iconRes: Int
) {
    MATCHED(
        titleRes = R.string.compare_status_matched,
        colorRes = R.color.step_match,
        iconRes = R.drawable.ic_check_circle
    ),
    STEP_MISMATCH(
        titleRes = R.string.compare_status_step_mismatch,
        colorRes = R.color.step_mismatch,
        iconRes = R.drawable.ic_warning
    ),
    MISSING(
        titleRes = R.string.compare_status_missing,
        colorRes = R.color.step_mismatch,
        iconRes = R.drawable.ic_error_outline
    ),
    UNEXPECTED(
        titleRes = R.string.compare_status_unexpected,
        colorRes = R.color.step_mismatch,
        iconRes = R.drawable.ic_warning
    )
}

data class ProductInventoryCompareUiItem(
    val domainItem: ProductInventoryCompareItem,
    val resolvedStep: StepDefinitionWithProcess? = null
) {
    val serialNumber: String get() = domainItem.serialNumber
    val status get() = domainItem.status
    val accountingStep: StepDefinitionWithProcess? get() = domainItem.accountingStepDefinition
    val inventoryStep: StepDefinitionWithProcess? get() = domainItem.inventoryStepDefinition

    val compareStatus: CompareStatus = when {
        accountingStep == null && inventoryStep != null -> CompareStatus.UNEXPECTED
        accountingStep != null && inventoryStep == null -> CompareStatus.MISSING
        accountingStep?.id == inventoryStep?.id -> CompareStatus.MATCHED
        else -> CompareStatus.STEP_MISMATCH
    }

    val isMismatch: Boolean get() = compareStatus != CompareStatus.MATCHED
}

/**
 * Extension-маппер из доменной модели в UI-модель
 */
fun ProductInventoryCompareItem.toUiItem(
    resolvedStep: StepDefinitionWithProcess? = null
): ProductInventoryCompareUiItem = ProductInventoryCompareUiItem(
    domainItem = this,
    resolvedStep = resolvedStep
)
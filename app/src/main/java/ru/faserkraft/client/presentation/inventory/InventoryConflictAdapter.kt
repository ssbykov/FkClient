package ru.faserkraft.client.presentation.inventory

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.ItemInventoryConflictBinding
import ru.faserkraft.client.presentation.product.detail.toUiProductStatus

class InventoryConflictAdapter(
    private val onActionClick: (ConflictItem) -> Unit,
) : ListAdapter<ConflictItem, InventoryConflictAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ItemInventoryConflictBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    inner class ViewHolder(
        private val b: ItemInventoryConflictBinding
    ) : RecyclerView.ViewHolder(b.root) {

        fun bind(conflict: ConflictItem) {
            val ctx = b.root.context
            val item = conflict.item

            // ── Серийный номер ──────────────────────────────────────
            b.tvSerialNumber.text = item.serialNumber

            // ── Блок «По базе данных» ───────────────────────────────
            b.tvDbStep.text = "${item.stepDefinition.order}. ${item.stepDefinition.name}"

            val dbStatusUi = item.status.toUiProductStatus()
            b.chipDbStatus.text = dbStatusUi.getTitle(ctx)
            b.chipDbStatus.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(ctx, dbStatusUi.bgColorRes))
            b.chipDbStatus.setTextColor(ContextCompat.getColor(ctx, dbStatusUi.textColorRes))

            // ── Тип конфликта + блок «При инвентаризации» ──────────
            when (conflict.conflictType) {
                ConflictType.MISSING -> {
                    b.tvConflictBadge.text = ctx.getString(R.string.conflict_missing)
                    b.tvConflictBadge.setBackgroundResource(R.drawable.bg_conflict_badge_missing)
                    b.tvConflictBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_wrong_text))

                    b.tvInvStep.text = ctx.getString(R.string.not_scanned)
                    b.tvInvStep.setTextColor(ContextCompat.getColor(ctx, R.color.step_mismatch))
                    b.tvPerformedAt.text = ctx.getString(R.string.performed_at_none)

                    b.tvActionHint.text = ctx.getString(R.string.hint_missing)
                    b.btnPrimaryAction.text = ctx.getString(R.string.action_mark_missing)
                }

                ConflictType.STEP_MISMATCH -> {
                    b.tvConflictBadge.setBackgroundResource(R.drawable.bg_conflict_badge_mismatch)
                    b.tvConflictBadge.setTextColor(ContextCompat.getColor(ctx, R.color.status_warning_text))

                    b.tvInvStep.text = conflict.scannedStep
                        ?.let { "${it.order}. ${it.name}" }
                        ?: ctx.getString(R.string.not_scanned)

                    b.tvPerformedAt.text = conflict.scannedAt
                        ?.let { ctx.getString(R.string.performed_at_label, it) }
                        ?: ctx.getString(R.string.performed_at_none)

                    if (conflict.dbAheadOfScan) {
                        b.tvConflictBadge.text = ctx.getString(R.string.conflict_db_ahead)
                        b.tvInvStep.setTextColor(ContextCompat.getColor(ctx, R.color.step_mismatch))
                        b.tvActionHint.text = ctx.getString(R.string.hint_db_ahead)
                        b.btnPrimaryAction.text = ctx.getString(R.string.action_correct_db_step)
                    } else {
                        b.tvConflictBadge.text = ctx.getString(R.string.conflict_scan_ahead)
                        b.tvInvStep.setTextColor(ContextCompat.getColor(ctx, R.color.step_match))
                        b.tvActionHint.text = ctx.getString(R.string.hint_scan_ahead)
                        b.btnPrimaryAction.text = ctx.getString(R.string.action_update_db_step)
                    }
                }
            }

            b.btnPrimaryAction.setOnClickListener { onActionClick(conflict) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<ConflictItem>() {
        override fun areItemsTheSame(a: ConflictItem, b: ConflictItem) =
            a.item.id == b.item.id
        override fun areContentsTheSame(a: ConflictItem, b: ConflictItem) =
            a == b
    }
}
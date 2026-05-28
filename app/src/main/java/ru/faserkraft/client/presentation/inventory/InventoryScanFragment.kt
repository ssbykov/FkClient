package ru.faserkraft.client.presentation.inventory

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.journeyapps.barcodescanner.BarcodeView
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentInventoryScanBinding
import ru.faserkraft.client.presentation.scanner.BaseScannerFragment
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.showErrorSnackbar

@AndroidEntryPoint
class InventoryScanFragment : BaseScannerFragment() {

    private val viewModel: InventoryViewModel by activityViewModels()

    private var _binding: FragmentInventoryScanBinding? = null
    private val binding get() = _binding!!

    override fun getScannerView(): BarcodeView? =
        _binding?.zxingBarcodeScanner?.barcodeView

    override fun onBarcodeDecoded(raw: String) {
        viewModel.onBarcodeScanned(raw)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentInventoryScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupManualInputMask()
        setupManualInputButton()
        observeState()
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resetScanHandled()
        binding.etManualInput.setText(R.string.uf_0000000)
        binding.etManualInput.clearFocus()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ---------- Manual input ----------

    private fun setupManualInputButton() {
        binding.tilManualInput.setEndIconOnClickListener {
            val current = binding.etManualInput.text.toString()
            if (current != "uf-0000000") {
                viewModel.onBarcodeScanned(current)
                binding.etManualInput.setText(R.string.uf_0000000)
                binding.etManualInput.clearFocus()
            }
        }
    }

    private fun setupManualInputMask() {
        val editText = binding.etManualInput
        editText.setOnClickListener { editText.setSelection(editText.text?.length ?: 0) }
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) editText.setSelection(editText.text?.length ?: 0)
        }
        editText.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting || s == null) return
                isFormatting = true
                val digits = s.toString().replace("\\D".toRegex(), "")
                val limited = if (digits.length > 7) digits.substring(digits.length - 7) else digits
                val formatted = "uf-${limited.padStart(7, '0')}"
                if (s.toString() != formatted) s.replace(0, s.length, formatted)
                isFormatting = false
            }
        })
    }

    // ---------- Observers ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.tvItemsCount.text = getString(
                R.string.inventory_scanned_count,
                state.currentInventoryItemCount
            )

            val lastItem = state.currentInventoryItems.maxByOrNull { it.scannedAt }
            b.llLastScanned.visibility = if (lastItem != null) View.VISIBLE else View.GONE
            b.tvLastScanned.text = lastItem?.serialNumber.orEmpty()

            // Показываем оверлей по центру как при isLoading, так и при isActionInProgress
            val showLoading = state.isLoading || state.isActionInProgress
            b.loadingOverlay.visibility = if (showLoading) View.VISIBLE else View.GONE

            if (showLoading) pauseScanner()
            else resumeScanner()
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                InventoryEvent.ShowConfirmDialog -> {
                    showConfirmDialogIfNotShown()
                }

                InventoryEvent.ItemUpserted -> {
                    (childFragmentManager.findFragmentByTag(InventoryConfirmBottomSheet.TAG)
                            as? InventoryConfirmBottomSheet)?.dismiss()
                }

                is InventoryEvent.ShowDuplicateWarning -> {
                    pauseScanner()

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Повторное сканирование")
                        .setMessage("Серийный номер ${event.serialNumber} уже отсканирован. Внести его в базу повторно?")
                        .setPositiveButton("Добавить") { _, _ ->
                            viewModel.forceAddDuplicate(event.serialNumber)
                        }
                        .setNegativeButton("Отмена") { dialog, _ ->
                            dialog.dismiss()
                            viewModel.resetScanHandled()
                            resumeScanner()
                        }
                        .setCancelable(false)
                        .show()
                }

                is InventoryEvent.ShowError -> {
                    showErrorSnackbar(event.message)
                    resumeScanner()
                }

                else -> Unit
            }
        }
    }

    private fun showConfirmDialogIfNotShown() {
        val existing = childFragmentManager
            .findFragmentByTag(InventoryConfirmBottomSheet.TAG)
        if (existing == null || !existing.isAdded) {
            InventoryConfirmBottomSheet()
                .show(childFragmentManager, InventoryConfirmBottomSheet.TAG)
        }
    }
}
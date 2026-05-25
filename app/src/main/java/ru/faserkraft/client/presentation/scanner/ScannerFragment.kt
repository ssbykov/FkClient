package ru.faserkraft.client.presentation.scanner

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.journeyapps.barcodescanner.BarcodeView
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentScannerBinding
import ru.faserkraft.client.presentation.app.AppEvent
import ru.faserkraft.client.presentation.app.AppViewModel
import ru.faserkraft.client.presentation.packaging.PackagingEvent
import ru.faserkraft.client.presentation.packaging.PackagingViewModel
import ru.faserkraft.client.presentation.product.detail.ProductEvent
import ru.faserkraft.client.presentation.product.detail.ProductViewModel
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

@AndroidEntryPoint
class ScannerFragment : BaseScannerFragment() {

    private val scannerViewModel: ScannerViewModel by activityViewModels()
    private val appViewModel: AppViewModel by activityViewModels()
    private val productViewModel: ProductViewModel by activityViewModels()
    private val packagingViewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    // ---------- BaseScannerFragment contract ----------

    override fun getScannerView(): BarcodeView? =
        _binding?.zxingBarcodeScanner?.barcodeView

    override fun onBarcodeDecoded(raw: String) {
        scannerViewModel.decodeQrCode(raw)
    }

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // запускает камеру
        setupManualInputMask()
        setupManualInputButton()
        observeScannerState()
        observeScannerEvents()
        observeAppEvents()
        observeAppErrors()
        observeProductEvents()
        observePackagingEvents()
    }

    override fun onResume() {
        super.onResume()
        scannerViewModel.resetHandled()
        binding.etManualInput.setText(R.string.uf_0000000)
        binding.etManualInput.clearFocus()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ---------- Observers ----------

    private fun observeScannerState() {
        collectFlow(scannerViewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow
            b.loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            if (state.isLoading) pauseScanner()
            else resumeScanner()
        }
    }

    private fun observeScannerEvents() {
        collectFlow(scannerViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is ScannerEvent.OpenProduct ->
                    productViewModel.loadProduct(event.code)

                is ScannerEvent.OpenPackaging ->
                    packagingViewModel.loadPackaging(event.code)

                is ScannerEvent.OpenDeviceRegistration -> {
                    pauseScanner()
                    appViewModel.registerDevice(event.request)
                }

                is ScannerEvent.ShowError ->
                    handleScannerError(event.message)
            }
        }
    }

    private fun observeAppEvents() {
        collectFlow(appViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                AppEvent.RegistrationCompleted ->
                    findNavController().navigateSafely(
                        R.id.action_scannerFragment_to_registrationFragment
                    )

                AppEvent.LogoutCompleted -> Unit
            }
        }
    }

    private fun observeAppErrors() {
        collectFlow(appViewModel.errorState) { message ->
            if (_binding == null || !isAdded) return@collectFlow
            showErrorSnackbar(message)
        }
    }

    private fun observeProductEvents() {
        collectFlow(productViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is ProductEvent.NavigateToProduct ->
                    findNavController().navigateSafely(R.id.action_scannerFragment_to_productFragment)

                is ProductEvent.NavigateToNewProduct ->
                    findNavController().navigateSafely(R.id.action_scannerFragment_to_newProductFragment)

                is ProductEvent.ShowError ->
                    handleScannerError(event.message)

                else -> Unit
            }
        }
    }

    private fun observePackagingEvents() {
        collectFlow(packagingViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                PackagingEvent.NavigateToPackaging ->
                    findNavController().navigateSafely(
                        R.id.action_scannerFragment_to_packagingFragment
                    )

                PackagingEvent.NavigateToNewPackaging ->
                    findNavController().navigateSafely(
                        R.id.action_scannerFragment_to_newPackagingFragment
                    )

                is PackagingEvent.ShowError ->
                    handleScannerError(event.message)

                PackagingEvent.NavigateToEdit,
                PackagingEvent.PackagingDeleted -> Unit
            }
        }
    }

    // ---------- Manual input ----------

    private fun setupManualInputButton() {
        binding.tilManualInput.setEndIconOnClickListener {
            val current = binding.etManualInput.text.toString()
            if (current != "uf-0000000") {
                scannerViewModel.decodeQrCode(current)
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

    private fun handleScannerError(message: String) {
        scannerViewModel.resetHandled()
        resumeScanner()
        showErrorSnackbar(message)
    }
}
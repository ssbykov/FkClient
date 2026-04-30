package ru.faserkraft.client.presentation.scanner

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentScannerBinding
import ru.faserkraft.client.presentation.packaging.PackagingEvent
import ru.faserkraft.client.presentation.packaging.PackagingViewModel
import ru.faserkraft.client.presentation.product.ProductEvent
import ru.faserkraft.client.presentation.product.ProductViewModel
import ru.faserkraft.client.presentation.ui.collectFlow

class ScannerFragment : Fragment() {

    private val scannerViewModel: ScannerViewModel by activityViewModels()
    private val productViewModel: ProductViewModel by activityViewModels()
    private val packagingViewModel: PackagingViewModel by activityViewModels()

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    private var activeDialog: AlertDialog? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (_binding == null) return@registerForActivityResult
        if (isGranted) startScanner()
        else Toast.makeText(
            requireContext(),
            "Camera permission is required to scan QR codes",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setupManualInputMask()
        setupManualInputButton()
        observeScannerState()
        observeScannerEvents()
        observeProductEvents()
        observePackagingEvents()
    }

    // ---------- Scanner state ----------

    private fun observeScannerState() {
        collectFlow(scannerViewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow
            b.loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            if (state.isLoading) b.zxingBarcodeScanner.pause()
            else b.zxingBarcodeScanner.resume()
        }
    }

    // ---------- Scanner events — только роутинг ----------

    private fun observeScannerEvents() {
        collectFlow(scannerViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is ScannerEvent.OpenProduct ->
                    productViewModel.loadProduct(event.serialNumber)

                is ScannerEvent.OpenPackaging ->
                    packagingViewModel.loadPackaging(event.serialNumber)

                is ScannerEvent.OpenDeviceRegistration ->
                    findNavController().navigate(
                        R.id.action_scannerFragment_to_registrationFragment
                    )

                is ScannerEvent.ShowError -> showError(event.message)
            }
        }
    }

    // ---------- Product events — навигация ----------

    private fun observeProductEvents() {
        collectFlow(productViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {

                is ProductEvent.NavigateToProduct ->
                    findNavController().navigate(
                        R.id.action_scannerFragment_to_productFragment
                    )

                is ProductEvent.NavigateToNewProduct ->
                    findNavController().navigate(
                        R.id.action_scannerFragment_to_newProductFragment
                    )

                is ProductEvent.ShowError -> showError(event.message)
            }
        }
    }

    // ---------- Packaging events — навигация ----------

    private fun observePackagingEvents() {
        collectFlow(packagingViewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is PackagingEvent.NavigateToPackaging ->
                    findNavController().navigate(
                        R.id.action_scannerFragment_to_packagingFragment
                    )

                is PackagingEvent.NavigateToNewPackaging ->
                    findNavController().navigate(
                        R.id.action_scannerFragment_to_newPackagingFragment
                    )

                is PackagingEvent.ShowError -> showError(event.message)
            }
        }
    }

    // ---------- Scanner ----------

    private fun startScanner() {
        binding.zxingBarcodeScanner.decodeContinuous { result ->
            val text = result?.text ?: return@decodeContinuous
            if (!isAdded || view == null || _binding == null) return@decodeContinuous
            scannerViewModel.decodeQrCode(text)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.zxingBarcodeScanner.resume()
        scannerViewModel.resetHandled()
        binding.etManualInput.setText(R.string.uf_0000000)
        binding.etManualInput.clearFocus()
    }

    override fun onPause() {
        super.onPause()
        binding.zxingBarcodeScanner.pause()
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
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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

    // ---------- Dialogs ----------

    private fun showError(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { d, _ ->
                scannerViewModel.resetHandled()
                d.dismiss()
                activeDialog = null
            }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        binding.zxingBarcodeScanner.pause()
        _binding = null
        super.onDestroyView()
    }
}
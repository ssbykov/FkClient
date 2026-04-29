//package ru.faserkraft.client.activity
//
//import android.Manifest
//import android.app.AlertDialog
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import androidx.navigation.fragment.findNavController
//import dagger.hilt.android.AndroidEntryPoint
//import kotlinx.coroutines.launch
//import ru.faserkraft.client.R
//import ru.faserkraft.client.databinding.FragmentScannerBinding
//import ru.faserkraft.client.viewmodel.ScannerViewModel
//
//@AndroidEntryPoint
//class ScannerFragment_OLD : Fragment() {
//
//    private val viewModel: ScannerViewModel by activityViewModels()
//
//    private var _binding: FragmentScannerBinding? = null
//    private val binding get() = _binding!!
//
//    private var activeDialog: AlertDialog? = null
//
//    private val cameraPermissionLauncher = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted: Boolean ->
//        // 🟡 ИСПРАВЛЕНИЕ 3: защита от отсутствия View
//        if (_binding == null) return@registerForActivityResult
//
//        if (isGranted) {
//            startScanner()
//        } else {
//            Toast.makeText(
//                requireContext(),
//                "Camera permission is required to scan QR codes",
//                Toast.LENGTH_LONG
//            ).show()
//        }
//    }
//
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentScannerBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        if (
//            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
//            == PackageManager.PERMISSION_GRANTED
//        ) {
//            startScanner()
//        } else {
//            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
//        }
//
//        setupManualInputMask()
//
//        // Пример обработки нажатия на иконку отправки:
//        binding.tilManualInput.setEndIconOnClickListener {
//            val currentNumber = binding.etManualInput.text.toString()
//            if (currentNumber != "uf-0000000") {
//                viewLifecycleOwner.lifecycleScope.launch {
//                    viewModel.decodeQrCode(currentNumber)
//                }
//                binding.etManualInput.setText(R.string.uf_0000000)
//                binding.etManualInput.clearFocus()
//            }
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.errorState.collect { msg ->
//
//                    if (!isAdded) return@collect
//
//                    activeDialog?.dismiss()
//                    activeDialog = AlertDialog.Builder(requireContext())
//                        .setMessage(msg)
//                        .setPositiveButton("ОК") { dialog, _ ->
//                            viewModel.resetIsHandled()
//                            dialog.dismiss()
//                            activeDialog = null
//                        }
//                        .also { it.setOnDismissListener { activeDialog = null } }
//                        .show()
//                }
//            }
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.events.collect { event ->
//
//                    if (_binding == null || !isAdded) return@collect
//
//                    when (event) {
//                        ScannerViewModel.UiEvent.NavigateToProduct ->
//                            findNavController().navigate(R.id.action_scannerFragment_to_productFragment)
//
//                        ScannerViewModel.UiEvent.NavigateToRegistration ->
//                            findNavController().navigate(R.id.action_scannerFragment_to_registrationFragment)
//
//                        ScannerViewModel.UiEvent.NavigateToNewProduct ->
//                            findNavController().navigate(R.id.action_scannerFragment_to_newProductFragment)
//
//                        ScannerViewModel.UiEvent.NavigateToNewPackaging ->
//                            findNavController().navigate(R.id.action_scannerFragment_to_newPackagingFragment)
//
//                        ScannerViewModel.UiEvent.NavigateToPackaging ->
//                            findNavController().navigate(R.id.action_scannerFragment_to_packagingFragment)
//                    }
//                }
//            }
//        }
//
//        viewModel.uiState.observe(viewLifecycleOwner) { state ->
//            // 🟡 ИСПРАВЛЕНИЕ 4: безопасный доступ к binding
//            val b = _binding ?: return@observe
//
//            b.loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE
//
//            if (state.isLoading) {
//                b.zxingBarcodeScanner.pause()
//            } else {
//                b.zxingBarcodeScanner.resume()
//            }
//        }
//    }
//
//    private fun startScanner() {
//        binding.zxingBarcodeScanner.decodeContinuous { result ->
//            val text = result?.text ?: return@decodeContinuous
//
//            if (!isAdded || view == null || _binding == null) return@decodeContinuous
//
//            viewLifecycleOwner.lifecycleScope.launch {
//                viewModel.decodeQrCode(text)
//            }
//        }
//    }
//
//    override fun onResume() {
//        super.onResume()
//        binding.zxingBarcodeScanner.resume()
//        viewModel.resetIsHandled()
//        binding.etManualInput.setText(R.string.uf_0000000)
//        binding.etManualInput.clearFocus()
//    }
//
//    override fun onPause() {
//        super.onPause()
//        binding.zxingBarcodeScanner.pause()
//    }
//
//    override fun onDestroyView() {
//        activeDialog?.dismiss()
//        activeDialog = null
//
//        binding.zxingBarcodeScanner.pause()
//        _binding = null
//        super.onDestroyView()
//    }
//
//    private fun setupManualInputMask() {
//        val editText = binding.etManualInput
//
//        editText.setOnClickListener {
//            editText.setSelection(editText.text?.length ?: 0)
//        }
//        editText.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus) editText.setSelection(editText.text?.length ?: 0)
//        }
//
//        editText.addTextChangedListener(object : TextWatcher {
//            private var isFormatting = false
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//
//            override fun afterTextChanged(s: Editable?) {
//                if (isFormatting || s == null) return
//                isFormatting = true
//
//                // 1. Извлекаем из текущего ввода абсолютно все цифры
//                val digits = s.toString().replace("\\D".toRegex(), "")
//
//                // 2. Ограничиваем длину 7 символами (берем последние 7, если ввели больше)
//                val limitedDigits =
//                    if (digits.length > 7) digits.substring(digits.length - 7) else digits
//
//                // 3. Добиваем нулями слева до 7 символов
//                val padded = limitedDigits.padStart(7, '0')
//                val formatted = "uf-$padded"
//
//                // 4. Если текст отличается от нужного формата — подменяем его
//                if (s.toString() != formatted) {
//                    s.replace(0, s.length, formatted)
//                }
//
//                isFormatting = false
//            }
//        })
//    }
//}
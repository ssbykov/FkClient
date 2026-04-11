package ru.faserkraft.client.activity

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentScannerBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

@AndroidEntryPoint
class ScannerFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentScannerBinding? = null
    private val binding get() = _binding!!

    // 🔴 ИСПРАВЛЕНИЕ 1: переменная для контроля диалогов
    private var activeDialog: AlertDialog? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        // 🟡 ИСПРАВЛЕНИЕ 3: защита от отсутствия View
        if (_binding == null) return@registerForActivityResult

        if (isGranted) {
            startScanner()
        } else {
            Toast.makeText(
                requireContext(),
                "Camera permission is required to scan QR codes",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScannerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startScanner()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    // 🔴 ИСПРАВЛЕНИЕ 1: защита от крэша и накопления
                    if (!isAdded) return@collect

                    activeDialog?.dismiss()
                    activeDialog = AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                            activeDialog = null
                        }
                        .also { it.setOnDismissListener { activeDialog = null } }
                        .show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    // 🔴 ИСПРАВЛЕНИЕ 2: защита навигации
                    if (_binding == null || !isAdded) return@collect

                    when (event) {
                        ScannerViewModel.UiEvent.NavigateToProduct ->
                            findNavController().navigate(R.id.action_scannerFragment_to_productFragment)

                        ScannerViewModel.UiEvent.NavigateToRegistration ->
                            findNavController().navigate(R.id.action_scannerFragment_to_registrationFragment)

                        ScannerViewModel.UiEvent.NavigateToNewProduct ->
                            findNavController().navigate(R.id.action_scannerFragment_to_newProductFragment)

                        ScannerViewModel.UiEvent.NavigateToNewPackaging ->
                            findNavController().navigate(R.id.action_scannerFragment_to_newPackagingFragment)

                        ScannerViewModel.UiEvent.NavigateToPackaging ->
                            findNavController().navigate(R.id.action_scannerFragment_to_packagingFragment)
                    }
                }
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            // 🟡 ИСПРАВЛЕНИЕ 4: безопасный доступ к binding
            val b = _binding ?: return@observe

            b.loadingOverlay.visibility = if (state.isLoading) View.VISIBLE else View.GONE

            if (state.isLoading) {
                b.zxingBarcodeScanner.pause()
            } else {
                b.zxingBarcodeScanner.resume()
            }
        }
    }

    private fun startScanner() {
        binding.zxingBarcodeScanner.decodeContinuous { result ->
            val text = result?.text ?: return@decodeContinuous

            if (!isAdded || view == null || _binding == null) return@decodeContinuous

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.decodeQrCode(text)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // onResume гарантирует, что View существует, поэтому здесь binding безопасен
        binding.zxingBarcodeScanner.resume()
        viewModel.resetIsHandled()
    }

    override fun onPause() {
        super.onPause()
        binding.zxingBarcodeScanner.pause()
    }

    override fun onDestroyView() {
        // 🔴 ИСПРАВЛЕНИЕ 1: закрываем диалог при уничтожении
        activeDialog?.dismiss()
        activeDialog = null

        binding.zxingBarcodeScanner.pause()
        _binding = null
        super.onDestroyView()
    }
}
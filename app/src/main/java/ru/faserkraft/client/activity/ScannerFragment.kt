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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
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
        ViewCompat.setOnApplyWindowInsetsListener(binding.scanResult) { scanView, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            scanView.updatePadding(bottom = bottomInset)
            insets
        }

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
                    AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        ScannerViewModel.UiEvent.NavigateToProduct ->
                            findNavController()
                                .navigate(R.id.action_scannerFragment_to_productFragment)

                        ScannerViewModel.UiEvent.NavigateToRegistration ->
                            findNavController()
                                .navigate(R.id.action_scannerFragment_to_registrationFragment)
                        ScannerViewModel.UiEvent.NavigateToNewProduct ->
                            findNavController()
                                .navigate(R.id.action_scannerFragment_to_newProductFragment)
                    }
                }
            }
        }
    }

    private fun startScanner() {
        binding.zxingBarcodeScanner.decodeContinuous { result ->
            val text = result?.text ?: return@decodeContinuous

            // если view уже уничтожена – просто игнорируем результат
            if (!isAdded || view == null) return@decodeContinuous

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.decodeQrCode(text)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.zxingBarcodeScanner.resume()
        viewModel.resetIsHandled()
    }

    override fun onPause() {
        super.onPause()
        binding.zxingBarcodeScanner.pause()
    }

    override fun onDestroyView() {
        binding.zxingBarcodeScanner.pause() // остановить сканер, чтобы не шли новые колбэки
        _binding = null
        super.onDestroyView()
    }
}
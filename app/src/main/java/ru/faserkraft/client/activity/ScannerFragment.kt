package ru.faserkraft.client.activity

import android.Manifest
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
import ru.faserkraft.client.databinding.FragmentScannerBinding

class ScannerFragment : Fragment() {

    private lateinit var binding: FragmentScannerBinding

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
        binding = FragmentScannerBinding.inflate(inflater, container, false)
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
    }

    private fun startScanner() {
        binding.zxingBarcodeScanner.decodeContinuous { result ->
            result?.let {
                binding.scanResult.text = it.text
            }
        }
    }

    override fun onResume() {
        super.onResume()
        binding.zxingBarcodeScanner.resume()
    }

    override fun onPause() {
        super.onPause()
        binding.zxingBarcodeScanner.pause()
    }


}
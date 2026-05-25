package ru.faserkraft.client.presentation.scanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.BarcodeView

abstract class BaseScannerFragment : Fragment() {

    protected var scannerStarted = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (_isViewAlive()) {
            if (isGranted) startScannerIfNeeded()
            else Toast.makeText(
                requireContext(),
                "Необходим доступ к камере для сканирования",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ---------- Абстрактный контракт ----------

    /** Вернуть ссылку на BarcodeView из binding текущего фрагмента */
    protected abstract fun getScannerView(): BarcodeView?

    /** Вызывается когда камера успешно считала штрих-код */
    protected abstract fun onBarcodeDecoded(raw: String)

    // ---------- Публичное API ----------

    protected fun checkAndStartCamera() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startScannerIfNeeded()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    protected fun resumeScanner() {
        if (scannerStarted) getScannerView()?.resume()
    }

    protected fun pauseScanner() {
        getScannerView()?.pause()
    }

    // ---------- Lifecycle ----------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndStartCamera()
    }

    override fun onResume() {
        super.onResume()
        if (scannerStarted) getScannerView()?.resume()
    }

    override fun onPause() {
        super.onPause()
        getScannerView()?.pause()
    }

    override fun onDestroyView() {
        getScannerView()?.pause()
        scannerStarted = false
        super.onDestroyView()
    }

    // ---------- Внутреннее ----------

    private fun startScannerIfNeeded() {
        if (scannerStarted || !_isViewAlive()) return
        scannerStarted = true

        getScannerView()?.decodeContinuous { result ->
            val text = result?.text ?: return@decodeContinuous
            if (!isAdded || view == null || !_isViewAlive()) return@decodeContinuous
            getScannerView()?.pause()
            onBarcodeDecoded(text)
        }
    }

    private fun _isViewAlive() = view != null && isAdded
}
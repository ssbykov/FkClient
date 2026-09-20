package ru.faserkraft.client.presentation.scanner

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faserkraft.client.domain.model.DeviceRequest
import ru.faserkraft.client.domain.qr.QrClassifier
import ru.faserkraft.client.domain.qr.QrInputSource
import ru.faserkraft.client.domain.qr.QrParseResult
import ru.faserkraft.client.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val qrClassifier: QrClassifier = mockk()
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setUp() {
        viewModel = ScannerViewModel(qrClassifier)
    }

    // ── initial state ─────────────────────────────────────────────────────────

    @Test
    fun `initial state - has correct defaults`() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertNull(state.lastScannedValue)
    }

    // ── decodeQrCode - Product ─────────────────────────────────────────────────

    @Test
    fun `decodeQrCode - emits OpenProduct on Product result with source`() = runTest {
        every { qrClassifier.classify("product-qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.OpenProduct("P-001", QrInputSource.CAMERA), awaitItem())
        }
    }

    @Test
    fun `decodeQrCode - resets isLoading after Product result`() = runTest {
        every { qrClassifier.classify("product-qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── decodeQrCode - Packaging ──────────────────────────────────────────────

    @Test
    fun `decodeQrCode - emits OpenPackaging on Packaging result`() = runTest {
        every { qrClassifier.classify("pack-qr") } returns QrParseResult.Packaging("PKG-007")

        viewModel.events.test {
            viewModel.decodeQrCode("pack-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.OpenPackaging("PKG-007"), awaitItem())
        }
    }

    // ── decodeQrCode - DeviceRegistration ─────────────────────────────────────

    @Test
    fun `decodeQrCode - emits OpenDeviceRegistration on DeviceRegistration result from CAMERA`() = runTest {
        val fakeRequest = DeviceRequest(
            deviceId = "device-001",
            model = "Pixel 7",
            manufacturer = "Google",
            token = "token-abc",
            password = "pass123",
            userId = 5
        )
        every { qrClassifier.classify("device-qr") } returns QrParseResult.DeviceRegistration(
            fakeRequest
        )

        viewModel.events.test {
            viewModel.decodeQrCode("device-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.OpenDeviceRegistration(fakeRequest), awaitItem())
        }
    }

    @Test
    fun `decodeQrCode - emits ShowError on DeviceRegistration result from MANUAL_INPUT`() = runTest {
        val fakeRequest = DeviceRequest(
            deviceId = "device-001",
            model = "Pixel 7",
            manufacturer = "Google",
            token = "token-abc",
            password = "pass123",
            userId = 5
        )
        every { qrClassifier.classify("device-qr") } returns QrParseResult.DeviceRegistration(
            fakeRequest
        )

        viewModel.events.test {
            viewModel.decodeQrCode("device-qr", QrInputSource.MANUAL_INPUT)
            advanceUntilIdle()

            assertEquals(
                ScannerEvent.ShowError("Регистрация устройства доступна только при сканировании QR-кода"),
                awaitItem()
            )
        }
    }

    // ── decodeQrCode - Unknown ────────────────────────────────────────────────

    @Test
    fun `decodeQrCode - emits ShowError on Unknown result`() = runTest {
        every { qrClassifier.classify("garbage") } returns QrParseResult.Unknown

        viewModel.events.test {
            viewModel.decodeQrCode("garbage", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.ShowError("Нераспознанный QR-код"), awaitItem())
        }
    }

    @Test
    fun `decodeQrCode - resets isLoading after Unknown result`() = runTest {
        every { qrClassifier.classify("garbage") } returns QrParseResult.Unknown

        viewModel.events.test {
            viewModel.decodeQrCode("garbage", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── decodeQrCode - Exception ──────────────────────────────────────────────

    @Test
    fun `decodeQrCode - emits ShowError when classifier throws`() = runTest {
        every { qrClassifier.classify(any()) } throws RuntimeException("parse failure")

        viewModel.events.test {
            viewModel.decodeQrCode("bad-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.ShowError("Ошибка при чтении QR-кода"), awaitItem())
        }
    }

    @Test
    fun `decodeQrCode - resets isLoading when classifier throws`() = runTest {
        every { qrClassifier.classify(any()) } throws RuntimeException("parse failure")

        viewModel.events.test {
            viewModel.decodeQrCode("bad-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── decodeQrCode - lastScannedValue ───────────────────────────────────────

    @Test
    fun `decodeQrCode - updates lastScannedValue in state`() = runTest {
        every { qrClassifier.classify("raw-code") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("raw-code", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals("raw-code", viewModel.uiState.value.lastScannedValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── isHandled guard ───────────────────────────────────────────────────────

    @Test
    fun `decodeQrCode - ignores second call while first is still handled`() = runTest {
        every { qrClassifier.classify("qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("qr", QrInputSource.CAMERA)
            viewModel.decodeQrCode("qr", QrInputSource.CAMERA)  // должен быть проигнорирован
            advanceUntilIdle()

            awaitItem()
            expectNoEvents()  // второй вызов не должен породить новое событие

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `decodeQrCode - classifier is called only once when guarded by isHandled`() = runTest {
        every { qrClassifier.classify("qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("qr", QrInputSource.CAMERA)
            viewModel.decodeQrCode("qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            verify(exactly = 1) { qrClassifier.classify("qr") }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `decodeQrCode - allows second scan after Unknown result resets isHandled`() = runTest {
        every { qrClassifier.classify("garbage") } returns QrParseResult.Unknown
        every { qrClassifier.classify("product-qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("garbage", QrInputSource.CAMERA)
            advanceUntilIdle()
            awaitItem()  // ShowError("Нераспознанный QR-код")

            // isHandled был сброшен для Unknown → должен работать следующий скан
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.OpenProduct("P-001", QrInputSource.CAMERA), awaitItem())
        }
    }

    @Test
    fun `decodeQrCode - allows second scan after exception resets isHandled`() = runTest {
        every { qrClassifier.classify("bad-qr") } throws RuntimeException()
        every { qrClassifier.classify("product-qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("bad-qr", QrInputSource.CAMERA)
            advanceUntilIdle()
            awaitItem()  // ShowError("Ошибка при чтении QR-кода")

            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.OpenProduct("P-001", QrInputSource.CAMERA), awaitItem())
        }
    }

    // ── resetHandled ──────────────────────────────────────────────────────────

    @Test
    fun `resetHandled - allows new scan after successful product scan`() = runTest {
        every { qrClassifier.classify("product-qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()
            awaitItem()

            viewModel.resetHandled()
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.OpenProduct("P-001", QrInputSource.CAMERA), awaitItem())
        }
    }

    // ── clearState ────────────────────────────────────────────────────────────

    @Test
    fun `clearState - resets state to defaults`() = runTest {
        every { qrClassifier.classify("product-qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()
            awaitItem()

            viewModel.clearState()

            val state = viewModel.uiState.value
            assertFalse(state.isLoading)
            assertNull(state.lastScannedValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearState - allows new scan after successful product scan`() = runTest {
        every { qrClassifier.classify("product-qr") } returns QrParseResult.Product("P-001")

        viewModel.events.test {
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()
            awaitItem()

            viewModel.clearState()
            viewModel.decodeQrCode("product-qr", QrInputSource.CAMERA)
            advanceUntilIdle()

            assertEquals(ScannerEvent.OpenProduct("P-001", QrInputSource.CAMERA), awaitItem())
        }
    }
}

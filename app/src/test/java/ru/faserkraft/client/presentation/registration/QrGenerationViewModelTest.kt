package ru.faserkraft.client.presentation.registration

import android.graphics.Bitmap
import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.usecase.employee.GetEmployeeQrContentUseCase
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.util.MainDispatcherRule
import ru.faserkraft.client.utils.QrCodeGeneratorWrapper

@OptIn(ExperimentalCoroutinesApi::class)
class QrGenerationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getEmployeesUseCase: GetEmployeesUseCase = mockk()
    private val getEmployeeQrContentUseCase: GetEmployeeQrContentUseCase = mockk()
    private val qrCodeGenerator: QrCodeGeneratorWrapper = mockk()

    private lateinit var viewModel: QrGenerationViewModel

    private val dummyEmployee = Employee(
        id = 1,
        name = "Иван",
        email = "ivan@test.com"
    )
    private val dummyEmployeeList = listOf(dummyEmployee)

    private val dummyBitmap: Bitmap = mockk(relaxed = true)

    @Before
    fun setUp() {
        viewModel = QrGenerationViewModel(
            getEmployeesUseCase = getEmployeesUseCase,
            getEmployeeQrContentUseCase = getEmployeeQrContentUseCase,
            qrCodeGenerator = qrCodeGenerator,
        )
    }

    @Test
    fun `initial state - has correct defaults`() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isActionInProgress)
        assertTrue(state.employees.isEmpty())
        assertNull(state.qrBitmap)
    }

    @Test
    fun `loadEmployees - updates state with employee list on success`() = runTest {
        coEvery { getEmployeesUseCase() } returns dummyEmployeeList

        viewModel.events.test {
            viewModel.loadEmployees()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyEmployeeList, state.employees)
            assertFalse(state.isLoading)
            assertNull(state.qrBitmap)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadEmployees - emits ShowError on failure`() = runTest {
        coEvery { getEmployeesUseCase() } throws RuntimeException("Network Error")

        viewModel.events.test {
            viewModel.loadEmployees()
            advanceUntilIdle()

            assertEquals(
                QrGenerationEvent.ShowError("Неизвестная ошибка"),
                awaitItem()
            )
            assertFalse(viewModel.uiState.value.isLoading)
        }
    }

    @Test
    fun `generateQr - requests qr content and passes it to generator`() = runTest {
        val expectedContent = "employee:42:secret-token"

        coEvery { getEmployeeQrContentUseCase(42) } returns expectedContent
        every { qrCodeGenerator.generate(expectedContent, any()) } returns dummyBitmap

        viewModel.generateQr(42)
        advanceUntilIdle()

        coVerify(exactly = 1) { getEmployeeQrContentUseCase(42) }
        verify(exactly = 1) { qrCodeGenerator.generate(expectedContent, any()) }
    }

    @Test
    fun `generateQr - emits ShowError when qr content loading fails`() = runTest {
        coEvery { getEmployeeQrContentUseCase(1) } throws RuntimeException("QR content error")

        viewModel.events.test {
            viewModel.generateQr(1)
            advanceUntilIdle()

            assertEquals(
                QrGenerationEvent.ShowError("Неизвестная ошибка"),
                awaitItem()
            )
        }
    }

    @Test
    fun `generateQr - emits ShowError when generator fails`() = runTest {
        coEvery { getEmployeeQrContentUseCase(1) } returns "qr-content-string"
        every { qrCodeGenerator.generate("qr-content-string", any()) } throws RuntimeException("Bitmap error")

        viewModel.events.test {
            viewModel.generateQr(1)
            advanceUntilIdle()

            assertEquals(
                QrGenerationEvent.ShowError("Неизвестная ошибка"),
                awaitItem()
            )
        }
    }

    @Test
    fun `generateQr - qrBitmap remains null when qr content loading fails`() = runTest {
        coEvery { getEmployeeQrContentUseCase(1) } throws RuntimeException()

        viewModel.events.test {
            viewModel.generateQr(1)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.qrBitmap)
            awaitItem()
        }
    }

    @Test
    fun `generateQr - qrBitmap remains null when generator fails`() = runTest {
        coEvery { getEmployeeQrContentUseCase(1) } returns "qr-content-string"
        every { qrCodeGenerator.generate("qr-content-string", any()) } throws RuntimeException()

        viewModel.events.test {
            viewModel.generateQr(1)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.qrBitmap)
            awaitItem()
        }
    }
}
package ru.faserkraft.client.presentation.product

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.domain.model.ProductsInventory
import ru.faserkraft.client.domain.model.Step
import ru.faserkraft.client.domain.model.StepDefinition
import ru.faserkraft.client.domain.model.StepStatus
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStatusUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsInventoryUseCase
import ru.faserkraft.client.presentation.product.inventory.InventoryViewModel
import ru.faserkraft.client.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Mocks ────────────────────────────────────────────────────────────────

    private val getProductsInventoryUseCase: GetProductsInventoryUseCase = mockk()
    private val getProductsByLastStepUseCase: GetProductsByLastStepUseCase = mockk()
    private val getProductsByStatusUseCase: GetProductsByStatusUseCase = mockk()
    private val appAuth: AppAuth = mockk(relaxed = true)

    private lateinit var viewModel: InventoryViewModel

    // ── Dummies ──────────────────────────────────────────────────────────────

    private val masterUserData = mockk<UserData>(relaxed = true) {
        every { role } returns UserRole.MASTER
    }

    private val dummyEmployee = Employee(id = 1, name = "Иван", email = "ivan@test.com")

    private val dummyStepDefinition = StepDefinition(
        id = 1, order = 1, name = "Шаг 1", nameGenitive = "Шага 1"
    )

    private val dummyProcess = Process(
        id = 1, name = "Процесс 1", description = "Описание",
        steps = listOf(dummyStepDefinition),
    )

    private val dummyStep = Step(
        id = 1, productId = 1, definition = dummyStepDefinition,
        status = StepStatus.DONE, performedBy = dummyEmployee, performedAt = "2026-05-10",
    )

    private val dummyProduct = Product(
        id = 1L, serialNumber = "SN-001", process = dummyProcess,
        createdAt = "2026-05-01", packagingSerialNumber = null,
        status = ProductStatus.NORMAL, steps = listOf(dummyStep),
    )

    private val dummyReworkProduct = dummyProduct.copy(
        id = 2L, serialNumber = "SN-REWORK", status = ProductStatus.REWORK
    )
    private val dummyScrapProduct = dummyProduct.copy(
        id = 3L, serialNumber = "SN-SCRAP", status = ProductStatus.SCRAP
    )
    private val dummyNotNormalProducts = listOf(dummyReworkProduct, dummyScrapProduct)

    private val dummyInventory = ProductsInventory(
        processId = 1, processName = "Процесс 1",
        stepDefinitionId = 1, stepName = "Шаг 1",
        stepNameGenitive = "Шага 1", count = 42,
    )
    private val dummyInventoryList = listOf(dummyInventory)

    @Before
    fun setUp() {
        every { appAuth.getRegistrationData() } returns masterUserData
        viewModel = buildViewModel()
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Test
    fun `init - sets userRole from appAuth`() {
        assertEquals(UserRole.MASTER, viewModel.uiState.value.userRole)
    }

    // ── loadProductsInventory ─────────────────────────────────────────────────

    @Test
    fun `loadProductsInventory - updates state on success`() = runTest {
        coEvery { getProductsInventoryUseCase() } returns dummyInventoryList

        viewModel.loadProductsInventory()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyInventoryList, state.productsInventory)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loadProductsInventory - isLoading is false after completion`() = runTest {
        coEvery { getProductsInventoryUseCase() } returns dummyInventoryList

        viewModel.loadProductsInventory()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadProductsInventory - sets errorMessage on failure`() = runTest {
        coEvery { getProductsInventoryUseCase() } throws RuntimeException()

        viewModel.loadProductsInventory()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Неизвестная ошибка", state.errorMessage)
    }

    // ── loadProductsByLastStep ────────────────────────────────────────────────

    @Test
    fun `loadProductsByLastStep - updates state on success`() = runTest {
        coEvery { getProductsByLastStepUseCase(1, 1) } returns listOf(dummyProduct)

        viewModel.loadProductsByLastStep(1, 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf(dummyProduct), state.productsInventoryByProcess)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loadProductsByLastStep - isLoading is false after completion`() = runTest {
        coEvery { getProductsByLastStepUseCase(1, 1) } returns listOf(dummyProduct)

        viewModel.loadProductsByLastStep(1, 1)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadProductsByLastStep - clears list before loading`() = runTest {
        coEvery { getProductsByLastStepUseCase(1, 1) } returns listOf(dummyProduct)

        viewModel.uiState.test {
            awaitItem() // initial

            viewModel.loadProductsByLastStep(1, 1)

            // первый emit после вызова: isLoading=true + list=empty
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertTrue(loadingState.productsInventoryByProcess.isEmpty())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadProductsByLastStep - sets errorMessage on failure`() = runTest {
        coEvery { getProductsByLastStepUseCase(any(), any()) } throws RuntimeException()

        viewModel.loadProductsByLastStep(1, 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Неизвестная ошибка", state.errorMessage)
    }

    // ── loadReworkScrapProducts ───────────────────────────────────────────────

    @Test
    fun `loadReworkScrapProducts - updates state on success`() = runTest {
        coEvery {
            getProductsByStatusUseCase(listOf(ProductStatus.REWORK, ProductStatus.SCRAP))
        } returns dummyNotNormalProducts

        viewModel.loadReworkScrapProducts()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyNotNormalProducts, state.reworkScrapProducts)
        assertFalse(state.isLoading)
        assertNull(state.errorMessage)
    }

    @Test
    fun `loadReworkScrapProducts - isLoading is false after completion`() = runTest {
        coEvery {
            getProductsByStatusUseCase(listOf(ProductStatus.REWORK, ProductStatus.SCRAP))
        } returns dummyNotNormalProducts

        viewModel.loadReworkScrapProducts()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `loadReworkScrapProducts - sets errorMessage on failure`() = runTest {
        coEvery { getProductsByStatusUseCase(any()) } throws RuntimeException("Network Error")

        viewModel.loadReworkScrapProducts()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Неизвестная ошибка", state.errorMessage)
    }

    // ── selectInventoryItem ───────────────────────────────────────────────────

    @Test
    fun `selectInventoryItem - updates selectedInventoryItem in state`() {
        viewModel.selectInventoryItem(dummyInventory)
        assertEquals(dummyInventory, viewModel.uiState.value.selectedInventoryItem)
    }

    // ── selectReworkScrapProduct ──────────────────────────────────────────────

    @Test
    fun `selectReworkScrapProduct - updates selectedScrapReworkItem in state`() {
        viewModel.selectReworkScrapProduct("Процесс 1", ProductStatus.REWORK)

        val selection = viewModel.uiState.value.selectedScrapReworkItem
        assertEquals("Процесс 1", selection?.processName)
        assertEquals(ProductStatus.REWORK, selection?.status)
    }

    // ── clearError ────────────────────────────────────────────────────────────

    @Test
    fun `clearError - resets errorMessage to null`() = runTest {
        coEvery { getProductsInventoryUseCase() } throws RuntimeException()

        viewModel.loadProductsInventory()
        advanceUntilIdle()
        assertEquals("Неизвестная ошибка", viewModel.uiState.value.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildViewModel() = InventoryViewModel(
        getProductsInventoryUseCase = getProductsInventoryUseCase,
        getProductsByLastStepUseCase = getProductsByLastStepUseCase,
        getProductsByStatusUseCase = getProductsByStatusUseCase,
        appAuth = appAuth,
    )
}
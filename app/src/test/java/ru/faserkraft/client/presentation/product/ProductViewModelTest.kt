package ru.faserkraft.client.presentation.product

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.domain.usecase.product.ChangeProductProcessUseCase
import ru.faserkraft.client.domain.usecase.product.ChangeProductStatusUseCase
import ru.faserkraft.client.domain.usecase.product.CreateProductUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStatusUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsInventoryUseCase
import ru.faserkraft.client.domain.usecase.step.ChangeStepPerformerUseCase
import ru.faserkraft.client.domain.usecase.step.CloseStepUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import ru.faserkraft.client.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Mocks ────────────────────────────────────────────────────────────────

    private val getProductUseCase: GetProductUseCase = mockk()
    private val createProductUseCase: CreateProductUseCase = mockk()
    private val changeProductStatusUseCase: ChangeProductStatusUseCase = mockk()
    private val changeProductProcessUseCase: ChangeProductProcessUseCase = mockk()
    private val closeStepUseCase: CloseStepUseCase = mockk()
    private val changeStepPerformerUseCase: ChangeStepPerformerUseCase = mockk()
    private val getProcessesUseCase: GetProcessesUseCase = mockk()
    private val getEmployeesUseCase: GetEmployeesUseCase = mockk()
    private val getProductsInventoryUseCase: GetProductsInventoryUseCase = mockk()
    private val getProductsByLastStepUseCase: GetProductsByLastStepUseCase = mockk()
    private val getProductsByStatusUseCase: GetProductsByStatusUseCase = mockk()
    private val appAuth: AppAuth = mockk(relaxed = true)
    private val sessionCoordinator: AppSessionCoordinator = mockk(relaxed = true)

    private lateinit var viewModel: ProductViewModel
    private val sessionEventsFlow = MutableSharedFlow<AppSessionEvent>()

    // ── Dummies ──────────────────────────────────────────────────────────────

    private val masterUserData = mockk<UserData>(relaxed = true) {
        every { role } returns UserRole.MASTER
    }

    private val dummyEmployee = Employee(id = 1, name = "Иван", email = "ivan@test.com")
    private val dummyEmployeeList = listOf(dummyEmployee)

    private val dummyStepDefinition = StepDefinition(
        id = 1, order = 1, name = "Шаг 1", nameGenitive = "Шага 1"
    )
    private val dummyStepDefinition2 = StepDefinition(
        id = 2, order = 2, name = "Шаг 2", nameGenitive = "Шага 2"
    )

    private val dummyStepPending = Step(
        id = 1,
        productId = 1,
        definition = dummyStepDefinition,
        status = StepStatus.PENDING,
        performedBy = null,
        performedAt = null,
    )
    private val dummyStepDone = Step(
        id = 2,
        productId = 1,
        definition = dummyStepDefinition2,
        status = StepStatus.DONE,
        performedBy = dummyEmployee,
        performedAt = "2026-05-10",
    )

    private val dummyProcess = Process(
        id = 1,
        name = "Процесс 1",
        description = "Описание",
        steps = listOf(dummyStepDefinition),
    )
    private val dummyProcessList = listOf(dummyProcess)

    private val dummyProduct = Product(
        id = 1L,
        serialNumber = "SN-001",
        process = dummyProcess,
        createdAt = "2026-05-01",
        packagingSerialNumber = null,
        status = ProductStatus.NORMAL,
        steps = listOf(dummyStepPending, dummyStepDone),
    )

    private val dummyReworkProduct = dummyProduct.copy(
        id = 2L,
        serialNumber = "SN-REWORK",
        status = ProductStatus.REWORK,
    )

    private val dummyScrapProduct = dummyProduct.copy(
        id = 3L,
        serialNumber = "SN-SCRAP",
        status = ProductStatus.SCRAP,
    )

    private val dummyNotNormalProducts = listOf(dummyReworkProduct, dummyScrapProduct)

    private val dummyInventory = ProductsInventory(
        processId = 1,
        processName = "Процесс 1",
        stepDefinitionId = 1,
        stepName = "Шаг 1",
        stepNameGenitive = "Шага 1",
        count = 42,
    )
    private val dummyInventoryList = listOf(dummyInventory)


    @Before
    fun setUp() {
        every { appAuth.getRegistrationData() } returns masterUserData
        every { sessionCoordinator.events } returns sessionEventsFlow

        viewModel = ProductViewModel(
            getProductUseCase = getProductUseCase,
            createProductUseCase = createProductUseCase,
            changeProductStatusUseCase = changeProductStatusUseCase,
            changeProductProcessUseCase = changeProductProcessUseCase,
            closeStepUseCase = closeStepUseCase,
            changeStepPerformerUseCase = changeStepPerformerUseCase,
            getProcessesUseCase = getProcessesUseCase,
            getEmployeesUseCase = getEmployeesUseCase,
            getProductsInventoryUseCase = getProductsInventoryUseCase,
            getProductsByLastStepUseCase = getProductsByLastStepUseCase,
            getProductsByStatusUseCase = getProductsByStatusUseCase,
            appAuth = appAuth,
            sessionCoordinator = sessionCoordinator,
        )
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Test
    fun `init - sets userRole from appAuth`() {
        assertEquals(UserRole.MASTER, viewModel.uiState.value.userRole)
    }

    // ── Session ──────────────────────────────────────────────────────────────

    @Test
    fun `observeSessionEvents - resets state on Logout`() = runTest {
        coEvery { getProductUseCase("SN-001") } returns dummyProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            sessionEventsFlow.emit(AppSessionEvent.Logout)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.product)
            assertNull(state.userRole)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── canEditProduct ────────────────────────────────────────────────────────

    @Test
    fun `canEditProduct - returns true for MASTER`() {
        assertTrue(UserRole.MASTER.canEditProduct())
    }

    @Test
    fun `canEditProduct - returns true for ADMIN`() {
        assertTrue(UserRole.ADMIN.canEditProduct())
    }

    @Test
    fun `canEditProduct - returns false for WORKER`() {
        assertFalse(UserRole.WORKER.canEditProduct())
    }

    @Test
    fun `canEditProduct - returns false for null`() {
        assertFalse(null.canEditProduct())
    }

    // ── loadProduct ───────────────────────────────────────────────────────────

    @Test
    fun `loadProduct - navigates to existing product and updates state`() = runTest {
        coEvery { getProductUseCase("SN-001") } returns dummyProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyProduct, state.product)
            assertNull(state.pendingSerialNumber)
            assertFalse(state.isLoading)
            assertEquals(ProductEvent.NavigateToProduct, awaitItem())
        }
    }

    @Test
    fun `loadProduct - navigates to new product when null returned`() = runTest {
        coEvery { getProductUseCase("SN-NEW") } returns null
        coEvery { getProcessesUseCase() } returns dummyProcessList

        viewModel.events.test {
            viewModel.loadProduct("SN-NEW")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertNull(state.product)
            assertEquals("SN-NEW", state.pendingSerialNumber)
            assertFalse(state.isLoading)
            assertEquals(ProductEvent.NavigateToNewProduct, awaitItem())
        }
    }

    @Test
    fun `loadProduct - emits ShowError on failure`() = runTest {
        coEvery { getProductUseCase(any()) } throws RuntimeException("Network Error")

        viewModel.events.test {
            viewModel.loadProduct("SN-ERR")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(ProductEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── updateProductState / selectedStep logic ───────────────────────────────

    @Test
    fun `loadProduct - selectedStep is first PENDING step`() = runTest {
        coEvery { getProductUseCase("SN-001") } returns dummyProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem()

            assertEquals(dummyStepPending, viewModel.uiState.value.selectedStep)
        }
    }

    @Test
    fun `loadProduct - selectedStep is last step when all are DONE`() = runTest {
        val allDoneProduct = dummyProduct.copy(
            steps = listOf(
                dummyStepDone,
                dummyStepDone.copy(id = 3, definition = dummyStepDefinition2)
            )
        )
        coEvery { getProductUseCase("SN-001") } returns allDoneProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem()

            assertEquals(allDoneProduct.steps.last(), viewModel.uiState.value.selectedStep)
        }
    }

    // ── createProduct ─────────────────────────────────────────────────────────

    @Test
    fun `createProduct - creates product and navigates to product screen`() = runTest {
        coEvery { createProductUseCase("SN-NEW", 1) } returns dummyProduct

        viewModel.events.test {
            viewModel.createProduct("SN-NEW", 1)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyProduct, state.product)
            assertNull(state.pendingSerialNumber)
            assertFalse(state.isActionInProgress)
            assertEquals(ProductEvent.NavigateToProduct, awaitItem())
        }
    }

    @Test
    fun `createProduct - emits ShowError on failure`() = runTest {
        coEvery { createProductUseCase(any(), any()) } throws RuntimeException()

        viewModel.events.test {
            viewModel.createProduct("SN-NEW", 1)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isActionInProgress)
            assertEquals(ProductEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── changeStatus ──────────────────────────────────────────────────────────

    @Test
    fun `changeStatus - updates product in state on success`() = runTest {
        val updatedProduct = dummyProduct.copy(status = ProductStatus.REWORK)
        coEvery { changeProductStatusUseCase(1L, ProductStatus.REWORK) } returns updatedProduct

        viewModel.events.test {
            viewModel.changeStatus(1L, ProductStatus.REWORK)
            advanceUntilIdle()

            assertEquals(ProductStatus.REWORK, viewModel.uiState.value.product?.status)
            assertFalse(viewModel.uiState.value.isActionInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `changeStatus - emits ShowError on failure`() = runTest {
        coEvery { changeProductStatusUseCase(any(), any()) } throws RuntimeException()

        viewModel.events.test {
            viewModel.changeStatus(1L, ProductStatus.REWORK)
            advanceUntilIdle()

            assertEquals(ProductEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── changeProcess ─────────────────────────────────────────────────────────

    @Test
    fun `changeProcess - updates product in state on success`() = runTest {
        val newProcess = Process(id = 2, name = "Процесс 2", description = "", steps = emptyList())
        val updatedProduct = dummyProduct.copy(process = newProcess)
        coEvery { changeProductProcessUseCase(1L, 2) } returns updatedProduct

        viewModel.events.test {
            viewModel.changeProcess(1L, 2)
            advanceUntilIdle()

            assertEquals(newProcess, viewModel.uiState.value.product?.process)
            assertFalse(viewModel.uiState.value.isActionInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onChangeStatusClicked ─────────────────────────────────────────────────

    @Test
    fun `onChangeStatusClicked - emits ShowConfirmationDialog when product set and role allows`() =
        runTest {
            coEvery { getProductUseCase("SN-001") } returns dummyProduct
            viewModel.events.test {
                viewModel.loadProduct("SN-001")
                advanceUntilIdle()
                awaitItem() // NavigateToProduct

                viewModel.onChangeStatusClicked()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is ProductEvent.ShowConfirmationDialog)
                assertEquals(
                    ConfirmationActionType.CHANGE_STATUS,
                    (event as ProductEvent.ShowConfirmationDialog).actionType
                )
            }
        }

    @Test
    fun `onChangeStatusClicked - does nothing when product is null`() = runTest {
        viewModel.events.test {
            viewModel.onChangeStatusClicked()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    @Test
    fun `onChangeStatusClicked - does nothing for WORKER role`() = runTest {
        val workerUser = mockk<UserData>(relaxed = true) {
            every { role } returns UserRole.WORKER
        }
        every { appAuth.getRegistrationData() } returns workerUser
        val vm = buildViewModel()

        coEvery { getProductUseCase("SN-001") } returns dummyProduct
        vm.events.test {
            vm.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            vm.onChangeStatusClicked()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    // ── onChangeProcessClicked ────────────────────────────────────────────────

    @Test
    fun `onChangeProcessClicked - emits ShowConfirmationDialog when product has no packaging`() =
        runTest {
            coEvery { getProductUseCase("SN-001") } returns dummyProduct
            viewModel.events.test {
                viewModel.loadProduct("SN-001")
                advanceUntilIdle()
                awaitItem() // NavigateToProduct

                viewModel.onChangeProcessClicked()
                advanceUntilIdle()

                val event = awaitItem()
                assertTrue(event is ProductEvent.ShowConfirmationDialog)
                assertEquals(
                    ConfirmationActionType.CHANGE_PROCESS,
                    (event as ProductEvent.ShowConfirmationDialog).actionType
                )
            }
        }

    @Test
    fun `onChangeProcessClicked - does nothing when product has packagingSerialNumber`() = runTest {
        val packedProduct = dummyProduct.copy(packagingSerialNumber = "PKG-001")
        coEvery { getProductUseCase("SN-001") } returns packedProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onChangeProcessClicked()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    // ── onCloseStepClicked ────────────────────────────────────────────────────

    @Test
    fun `onCloseStepClicked - emits ShowConfirmationDialog for normal PENDING step`() = runTest {
        coEvery { getProductUseCase("SN-001") } returns dummyProduct
        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onCloseStepClicked()
            advanceUntilIdle()

            val event = awaitItem()
            assertTrue(event is ProductEvent.ShowConfirmationDialog)
            assertEquals(
                ConfirmationActionType.CLOSE_STEP,
                (event as ProductEvent.ShowConfirmationDialog).actionType
            )
        }
    }

    @Test
    fun `onCloseStepClicked - emits ShowError when product is REWORK`() = runTest {
        val reworkProduct = dummyProduct.copy(status = ProductStatus.REWORK)
        coEvery { getProductUseCase("SN-001") } returns reworkProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onCloseStepClicked()
            advanceUntilIdle()

            val event = awaitItem() as ProductEvent.ShowError
            assertTrue(event.message.contains("РЕМОНТ"))
        }
    }

    @Test
    fun `onCloseStepClicked - emits ShowError when product is SCRAP`() = runTest {
        val scrapProduct = dummyProduct.copy(status = ProductStatus.SCRAP)
        coEvery { getProductUseCase("SN-001") } returns scrapProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onCloseStepClicked()
            advanceUntilIdle()

            val event = awaitItem() as ProductEvent.ShowError
            assertTrue(event.message.contains("БРАК"))
        }
    }

    @Test
    fun `onCloseStepClicked - emits ShowError when step is already DONE`() = runTest {
        val allDoneProduct = dummyProduct.copy(steps = listOf(dummyStepDone))
        coEvery { getProductUseCase("SN-001") } returns allDoneProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onCloseStepClicked()
            advanceUntilIdle()

            assertEquals(ProductEvent.ShowError("Этап уже выполнен"), awaitItem())
        }
    }

    @Test
    fun `onCloseStepClicked - does nothing when no step selected`() = runTest {
        coEvery { getProductUseCase("SN-001") } returns dummyProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.selectStep(dummyStepPending.copy(id = 0)) // step.id == 0
            viewModel.onCloseStepClicked()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    // ── onDialogConfirmed ─────────────────────────────────────────────────────

    @Test
    fun `onDialogConfirmed CHANGE_STATUS - emits NavigateToEditStatus`() = runTest {
        coEvery { getProductUseCase("SN-001") } returns dummyProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onDialogConfirmed(ConfirmationActionType.CHANGE_STATUS, null)
            advanceUntilIdle()

            assertEquals(ProductEvent.NavigateToEditStatus(1L), awaitItem())
        }
    }

    @Test
    fun `onDialogConfirmed CHANGE_PROCESS - emits NavigateToEditProcess and loads processes`() =
        runTest {
            coEvery { getProductUseCase("SN-001") } returns dummyProduct
            coEvery { getProcessesUseCase() } returns dummyProcessList

            viewModel.events.test {
                viewModel.loadProduct("SN-001")
                advanceUntilIdle()
                awaitItem() // NavigateToProduct

                viewModel.onDialogConfirmed(ConfirmationActionType.CHANGE_PROCESS, null)
                advanceUntilIdle()

                assertEquals(dummyProcessList, viewModel.uiState.value.processes)
                assertEquals(ProductEvent.NavigateToEditProcess(1L), awaitItem())
            }
        }

    @Test
    fun `onDialogConfirmed CLOSE_STEP - calls closeStep`() = runTest {
        val closedStep = dummyStepPending.copy(status = StepStatus.DONE)
        val updatedProduct = dummyProduct.copy(
            steps = listOf(closedStep, dummyStepDone)
        )
        coEvery { getProductUseCase("SN-001") } returns dummyProduct
        coEvery { closeStepUseCase(dummyStepPending.id) } returns updatedProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onDialogConfirmed(ConfirmationActionType.CLOSE_STEP, dummyStepPending)
            advanceUntilIdle()

            assertEquals(updatedProduct, viewModel.uiState.value.product)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── onPackagingClicked ────────────────────────────────────────────────────

    @Test
    fun `onPackagingClicked - emits NavigateToPackaging with serial`() = runTest {
        val packedProduct = dummyProduct.copy(packagingSerialNumber = "PKG-001")
        coEvery { getProductUseCase("SN-001") } returns packedProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onPackagingClicked()
            advanceUntilIdle()

            assertEquals(ProductEvent.NavigateToPackaging("PKG-001"), awaitItem())
        }
    }

    @Test
    fun `onPackagingClicked - does nothing when packagingSerialNumber is null`() = runTest {
        coEvery { getProductUseCase("SN-001") } returns dummyProduct

        viewModel.events.test {
            viewModel.loadProduct("SN-001")
            advanceUntilIdle()
            awaitItem() // NavigateToProduct

            viewModel.onPackagingClicked()
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    // ── closeStep ─────────────────────────────────────────────────────────────

    @Test
    fun `closeStep - updates product and selectedStep on success`() = runTest {
        val closedStep = dummyStepPending.copy(status = StepStatus.DONE)
        val updatedProduct = dummyProduct.copy(steps = listOf(closedStep, dummyStepDone))
        coEvery { closeStepUseCase(1) } returns updatedProduct

        viewModel.events.test {
            viewModel.closeStep(dummyStepPending)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(updatedProduct, state.product)
            assertFalse(state.isActionInProgress)
            assertNotNull(state.selectedStep)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `closeStep - ignores step with id 0`() = runTest {
        viewModel.events.test {
            viewModel.closeStep(dummyStepPending.copy(id = 0))
            advanceUntilIdle()

            expectNoEvents()
        }
    }

    // ── changeStepPerformer ───────────────────────────────────────────────────

    @Test
    fun `changeStepPerformer - updates product in state on success`() = runTest {
        val updatedProduct = dummyProduct.copy(
            steps = listOf(dummyStepPending.copy(performedBy = dummyEmployee))
        )
        coEvery { changeStepPerformerUseCase(1, 1) } returns updatedProduct

        viewModel.events.test {
            viewModel.changeStepPerformer(1, 1)
            advanceUntilIdle()

            assertEquals(updatedProduct, viewModel.uiState.value.product)
            assertFalse(viewModel.uiState.value.isActionInProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── loadProcesses & loadEmployees ─────────────────────────────────────────

    @Test
    fun `loadProcesses - updates state with process list on success`() = runTest {
        coEvery { getProcessesUseCase() } returns dummyProcessList

        viewModel.events.test {
            viewModel.loadProcesses()
            advanceUntilIdle()

            assertEquals(dummyProcessList, viewModel.uiState.value.processes)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadEmployees - updates state with employee list on success`() = runTest {
        coEvery { getEmployeesUseCase() } returns dummyEmployeeList

        viewModel.events.test {
            viewModel.loadEmployees()
            advanceUntilIdle()

            assertEquals(dummyEmployeeList, viewModel.uiState.value.employees)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── loadProductsInventory ─────────────────────────────────────────────────

    @Test
    fun `loadProductsInventory - updates state on success`() = runTest {
        coEvery { getProductsInventoryUseCase() } returns dummyInventoryList

        viewModel.events.test {
            viewModel.loadProductsInventory()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyInventoryList, state.productsInventory)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadProductsInventory - emits ShowError on failure`() = runTest {
        coEvery { getProductsInventoryUseCase() } throws RuntimeException()

        viewModel.events.test {
            viewModel.loadProductsInventory()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(ProductEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── loadReworkScrapProducts ───────────────────────────────────────────────

    @Test
    fun `loadReworkScrapProducts - updates state on success`() = runTest {
        coEvery {
            getProductsByStatusUseCase(listOf(ProductStatus.REWORK, ProductStatus.SCRAP))
        } returns dummyNotNormalProducts

        viewModel.events.test {
            viewModel.loadReworkScrapProducts()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyNotNormalProducts, state.reworkScrapProducts)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadReworkScrapProducts - emits ShowError on failure`() = runTest {
        coEvery {
            getProductsByStatusUseCase(any())
        } throws RuntimeException("Network Error")

        viewModel.events.test {
            viewModel.loadReworkScrapProducts()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(ProductEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── loadProductsByLastStep ────────────────────────────────────────────────

    @Test
    fun `loadProductsByLastStep - updates state on success`() = runTest {
        coEvery { getProductsByLastStepUseCase(1, 1) } returns listOf(dummyProduct)

        viewModel.events.test {
            viewModel.loadProductsByLastStep(1, 1)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(listOf(dummyProduct), state.productsInventoryByProcess)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadProductsByLastStep - clears list before loading`() = runTest {
        coEvery { getProductsByLastStepUseCase(1, 1) } returns listOf(dummyProduct)

        viewModel.loadProductsByLastStep(1, 1)
        assertTrue(viewModel.uiState.value.productsInventoryByProcess.isEmpty())
    }

    // ── selectStep & selectInventoryItem ──────────────────────────────────────

    @Test
    fun `selectStep - updates selectedStep in state`() {
        viewModel.selectStep(dummyStepDone)
        assertEquals(dummyStepDone, viewModel.uiState.value.selectedStep)
    }

    @Test
    fun `selectInventoryItem - updates selectedInventoryItem in state`() {
        viewModel.selectInventoryItem(dummyInventory)
        assertEquals(dummyInventory, viewModel.uiState.value.selectedInventoryItem)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildViewModel() = ProductViewModel(
        getProductUseCase = getProductUseCase,
        createProductUseCase = createProductUseCase,
        changeProductStatusUseCase = changeProductStatusUseCase,
        changeProductProcessUseCase = changeProductProcessUseCase,
        closeStepUseCase = closeStepUseCase,
        changeStepPerformerUseCase = changeStepPerformerUseCase,
        getProcessesUseCase = getProcessesUseCase,
        getEmployeesUseCase = getEmployeesUseCase,
        getProductsInventoryUseCase = getProductsInventoryUseCase,
        getProductsByLastStepUseCase = getProductsByLastStepUseCase,
        getProductsByStatusUseCase = getProductsByStatusUseCase,
        appAuth = appAuth,
        sessionCoordinator = sessionCoordinator,
    )
}
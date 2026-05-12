package ru.faserkraft.client.presentation.packaging

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.FinishedProcess
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.model.ProductShort          // ← заменён импорт
import ru.faserkraft.client.domain.model.ProductStatus         // ← добавлен импорт
import ru.faserkraft.client.domain.model.UserData
import ru.faserkraft.client.domain.usecase.packaging.CreatePackagingUseCase
import ru.faserkraft.client.domain.usecase.packaging.DeletePackagingUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingInStorageUseCase
import ru.faserkraft.client.domain.usecase.packaging.GetPackagingUseCase
import ru.faserkraft.client.domain.usecase.product.GetFinishedProductsUseCase
import ru.faserkraft.client.presentation.app.AppSessionCoordinator
import ru.faserkraft.client.presentation.app.AppSessionEvent
import ru.faserkraft.client.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class PackagingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── Mocks ────────────────────────────────────────────────────────────────

    private val getPackagingUseCase: GetPackagingUseCase = mockk()
    private val createPackagingUseCase: CreatePackagingUseCase = mockk()
    private val deletePackagingUseCase: DeletePackagingUseCase = mockk()
    private val getPackagingInStorageUseCase: GetPackagingInStorageUseCase = mockk()
    private val getFinishedProductsUseCase: GetFinishedProductsUseCase = mockk()
    private val appAuth: AppAuth = mockk(relaxed = true)
    private val sessionCoordinator: AppSessionCoordinator = mockk(relaxed = true)

    private lateinit var viewModel: PackagingViewModel

    private val sessionEventsFlow = MutableSharedFlow<AppSessionEvent>()

    // ── Dummies ──────────────────────────────────────────────────────────────

    private val dummyUserData = mockk<UserData>(relaxed = true)
    private val dummyPackaging = Packaging(
        id = 1,
        serialNumber = "SN-123",
        performedBy = null,
        performedAt = null,
        orderId = null,
        products = emptyList()
    )
    private val dummyPackagingList = listOf(dummyPackaging)

    private val dummyProcess = FinishedProcess(
        id = 1,
        name = "Сборка",
        sizeTypeId = 2,
        sizeTypeName = "Стандарт",
        packagingCount = 10
    )
    private val dummyProduct = ProductShort(      // ← FinishedProduct → ProductShort
        id = 1,
        serialNumber = "PRD-001",
        process = dummyProcess,
        status = ProductStatus.NORMAL             // ← добавлен статус
    )
    private val dummyProductList = listOf(dummyProduct)

    @Before
    fun setUp() {
        every { appAuth.getRegistrationData() } returns dummyUserData
        every { sessionCoordinator.events } returns sessionEventsFlow

        viewModel = PackagingViewModel(
            getPackagingUseCase = getPackagingUseCase,
            createPackagingUseCase = createPackagingUseCase,
            deletePackagingUseCase = deletePackagingUseCase,
            getPackagingInStorageUseCase = getPackagingInStorageUseCase,
            getFinishedProductsUseCase = getFinishedProductsUseCase,
            appAuth = appAuth,
            sessionCoordinator = sessionCoordinator,
        )
    }

    // ── Init & Session ───────────────────────────────────────────────────────

    @Test
    fun `init - loads currentUser from appAuth`() = runTest {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyUserData, state.currentUser)
    }

    @Test
    fun `observeSessionEvents - resets state on Logout event`() = runTest {
        viewModel.setPackaging(dummyPackaging)
        advanceUntilIdle()

        sessionEventsFlow.emit(AppSessionEvent.Logout)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.currentPackaging)
    }

    // ── loadPackaging ────────────────────────────────────────────────────────

    @Test
    fun `loadPackaging - sets packaging and emits NavigateToPackaging if found`() = runTest {
        coEvery { getPackagingUseCase("SN-123") } returns dummyPackaging

        viewModel.events.test {
            viewModel.loadPackaging("SN-123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyPackaging, state.currentPackaging)
            assertFalse(state.isLoading)

            assertEquals(PackagingEvent.NavigateToPackaging, awaitItem())
        }
    }

    @Test
    fun `loadPackaging - creates new packaging and emits NavigateToNewPackaging if null`() = runTest {
        coEvery { getPackagingUseCase("SN-123") } returns null
        coEvery { getFinishedProductsUseCase() } returns dummyProductList

        viewModel.events.test {
            viewModel.loadPackaging("SN-123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            val current = state.currentPackaging
            assertNotNull(current)
            assertEquals("SN-123", current?.serialNumber)
            assertEquals(0, current?.id)
            assertFalse(state.isLoading)

            coVerify { getFinishedProductsUseCase() }
            assertEquals(PackagingEvent.NavigateToNewPackaging, awaitItem())
        }
    }

    @Test
    fun `loadPackaging - creates new packaging if HttpException 404 is thrown`() = runTest {
        val httpException = mockk<HttpException>(relaxed = true) {
            every { code() } returns 404
        }
        coEvery { getPackagingUseCase("SN-123") } throws httpException
        coEvery { getFinishedProductsUseCase() } returns dummyProductList

        viewModel.events.test {
            viewModel.loadPackaging("SN-123")
            advanceUntilIdle()

            val state = viewModel.uiState.value
            val current = state.currentPackaging
            assertNotNull(current)
            assertEquals("SN-123", current?.serialNumber)
            assertFalse(state.isLoading)

            assertEquals(PackagingEvent.NavigateToNewPackaging, awaitItem())
        }
    }

    @Test
    fun `loadPackaging - emits ShowError if random Exception is thrown`() = runTest {
        coEvery { getPackagingUseCase("SN-123") } throws RuntimeException("Network Error")

        viewModel.events.test {
            viewModel.loadPackaging("SN-123")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(PackagingEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── createPackaging ──────────────────────────────────────────────────────

    @Test
    fun `createPackaging - sets packaging and emits NavigateToPackaging on success`() = runTest {
        val productIds = listOf(1, 2)
        coEvery { createPackagingUseCase("SN-123", productIds) } returns dummyPackaging

        viewModel.events.test {
            viewModel.createPackaging("SN-123", productIds)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(dummyPackaging, state.currentPackaging)
            assertFalse(state.isActionInProgress)

            assertEquals(PackagingEvent.NavigateToPackaging, awaitItem())
        }
    }

    @Test
    fun `createPackaging - emits ShowError on failure`() = runTest {
        val productIds = listOf(1, 2)
        coEvery { createPackagingUseCase("SN-123", productIds) } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.createPackaging("SN-123", productIds)
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isActionInProgress)
            assertEquals(PackagingEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── onEditClicked ────────────────────────────────────────────────────────

    @Test
    fun `onEditClicked - emits NavigateToEdit event`() = runTest {
        viewModel.events.test {
            viewModel.onEditClicked()
            advanceUntilIdle()

            assertEquals(PackagingEvent.NavigateToEdit, awaitItem())
        }
    }

    // ── deletePackaging ──────────────────────────────────────────────────────

    @Test
    fun `deletePackaging - clears currentPackaging and emits PackagingDeleted on success`() = runTest {
        viewModel.setPackaging(dummyPackaging)
        advanceUntilIdle()

        coEvery { deletePackagingUseCase("SN-123") } returns Unit

        viewModel.events.test {
            awaitItem() // Пропускаем NavigateToPackaging от setPackaging()

            viewModel.deletePackaging("SN-123")
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.currentPackaging)
            assertFalse(viewModel.uiState.value.isActionInProgress)

            assertEquals(PackagingEvent.PackagingDeleted, awaitItem())
        }
    }

    @Test
    fun `deletePackaging - emits ShowError on failure`() = runTest {
        coEvery { deletePackagingUseCase("SN-123") } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.deletePackaging("SN-123")
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isActionInProgress)
            assertEquals(PackagingEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── loadPackagingInStorage ───────────────────────────────────────────────

    @Test
    fun `loadPackagingInStorage - updates state with packaging list on success`() = runTest {
        coEvery { getPackagingInStorageUseCase() } returns dummyPackagingList

        viewModel.loadPackagingInStorage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyPackagingList, state.packagingInStorage)
        assertFalse(state.isLoading)
    }

    @Test
    fun `loadPackagingInStorage - emits ShowError on failure`() = runTest {
        coEvery { getPackagingInStorageUseCase() } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.loadPackagingInStorage()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(PackagingEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }

    // ── loadAvailableProducts ────────────────────────────────────────────────

    @Test
    fun `loadAvailableProducts - updates state with product list on success`() = runTest {
        coEvery { getFinishedProductsUseCase() } returns dummyProductList

        viewModel.loadAvailableProducts()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(dummyProductList, state.availableProducts)
    }

    @Test
    fun `loadAvailableProducts - emits ShowError on failure`() = runTest {
        coEvery { getFinishedProductsUseCase() } throws RuntimeException("Error")

        viewModel.events.test {
            viewModel.loadAvailableProducts()
            advanceUntilIdle()

            assertEquals(PackagingEvent.ShowError("Неизвестная ошибка"), awaitItem())
        }
    }
}
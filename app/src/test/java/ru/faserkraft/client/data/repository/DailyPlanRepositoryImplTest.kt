package ru.faserkraft.client.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import ru.faserkraft.client.data.dto.DailyPlanCopyDto
import ru.faserkraft.client.data.dto.DailyPlanStepCreateDto
import ru.faserkraft.client.data.dto.DailyPlanStepUpdateDto
import ru.faserkraft.client.data.dto.DayPlanDto
import ru.faserkraft.client.data.dto.DayPlanStepDto
import ru.faserkraft.client.data.dto.EmployeeDto
import ru.faserkraft.client.data.dto.StepDefinitionDto
import ru.faserkraft.client.data.dto.TemplateDto
import ru.faserkraft.client.data.dto.UserDto
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.util.FakeLogger
import java.io.IOException

class DailyPlanRepositoryImplTest {

    private lateinit var api: Api
    private lateinit var fakeLogger: FakeLogger
    private lateinit var repository: DailyPlanRepositoryImpl

    // ─── fixtures ─────────────────────────────────────────────────────────────

    private val stepDefinitionDto = StepDefinitionDto(
        id = 10,
        order = 1,
        template = TemplateDto(name = "Cutting", nameGenitive = "Cutting (gen)"),
    )

    private val stepDto = DayPlanStepDto(
        id = 1,
        dailyPlanId = 100,
        stepDefinitionId = 10,
        plannedQuantity = 50,
        actualQuantity = 20,
        workProcess = "active",
        stepDefinition = stepDefinitionDto,
    )

    private val employeeDto = EmployeeDto(
        id = 5,
        name = "Ivan",
        user = UserDto(id = 42, email = "ivan@test.com"),
    )

    private val planDto = DayPlanDto(
        id = 100,
        employeeId = 5,
        date = "2024-01-15",
        employee = employeeDto,
        steps = listOf(stepDto),
    )

    @Before
    fun setUp() {
        api = mockk()
        fakeLogger = FakeLogger()
        repository = DailyPlanRepositoryImpl(api, fakeLogger)
    }

    // ─── getDayPlans ──────────────────────────────────────────────────────────

    @Test
    fun `getDayPlans - returns list on success`() = runTest {
        coEvery { api.getDayPlans(any()) } returns Response.success(listOf(planDto))

        val result = repository.getDayPlans("2024-01-15")

        assertEquals(1, result.size)
        assertEquals(100, result[0].id)
        coVerify(exactly = 1) { api.getDayPlans("2024-01-15") }
        assertTrue(fakeLogger.errors.isEmpty())
    }

    @Test
    fun `getDayPlans - returns empty list when response body is null`() = runTest {
        coEvery { api.getDayPlans(any()) } returns Response.success(null)

        val result = repository.getDayPlans("2024-01-15")

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `getDayPlans - returns empty list when server returns empty list`() = runTest {
        coEvery { api.getDayPlans(any()) } returns Response.success(emptyList())

        val result = repository.getDayPlans("2024-01-15")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getDayPlans - throws AppError ApiError on HTTP 500`() = runTest {
        coEvery { api.getDayPlans(any()) } returns makeErrorResponse(500)

        val ex = catchError<AppError.ApiError> { repository.getDayPlans("2024-01-15") }

        assertNotNull(ex)
        assertEquals(500, ex!!.status)
        assertTrue(fakeLogger.errors.any { it.message.contains("500") })
    }

    @Test
    fun `getDayPlans - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.getDayPlans(any()) } throws IOException("timeout")

        val ex = catchError<AppError.NetworkError> { repository.getDayPlans("2024-01-15") }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
    }

    // ─── addStep ──────────────────────────────────────────────────────────────

    @Test
    fun `addStep - returns updated list on success`() = runTest {
        coEvery { api.addStepToDailyPlan(any()) } returns Response.success(listOf(planDto))

        val result = repository.addStep(
            planDate = "2024-01-15",
            employeeId = 5,
            stepId = 10,
            plannedQuantity = 50,
        )

        assertEquals(1, result.size)
        coVerify(exactly = 1) {
            api.addStepToDailyPlan(
                DailyPlanStepCreateDto(
                    planDate = "2024-01-15",
                    stepId = 10,
                    employeeId = 5,
                    plannedQuantity = 50,
                )
            )
        }
    }

    @Test
    fun `addStep - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.addStepToDailyPlan(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> {
            repository.addStep("2024-01-15", 5, 10, 50)
        }

        assertNotNull(ex)
    }

    @Test
    fun `addStep - throws AppError ApiError on HTTP 422`() = runTest {
        coEvery { api.addStepToDailyPlan(any()) } returns makeErrorResponse(422)

        val ex = catchError<AppError.ApiError> {
            repository.addStep("2024-01-15", 5, 10, 50)
        }

        assertNotNull(ex)
        assertEquals(422, ex!!.status)
    }

    // ─── updateStep ───────────────────────────────────────────────────────────

    @Test
    fun `updateStep - returns updated list on success`() = runTest {
        coEvery { api.updateStepInDailyPlan(any()) } returns Response.success(listOf(planDto))

        val result = repository.updateStep(
            stepId = 1,
            planDate = "2024-01-15",
            stepDefinitionId = 10,
            employeeId = 5,
            plannedQuantity = 75,
        )

        assertEquals(1, result.size)
        coVerify(exactly = 1) {
            api.updateStepInDailyPlan(
                DailyPlanStepUpdateDto(
                    stepId = 1,
                    planDate = "2024-01-15",
                    stepDefinitionId = 10,
                    employeeId = 5,
                    plannedQuantity = 75,
                )
            )
        }
    }

    @Test
    fun `updateStep - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.updateStepInDailyPlan(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> {
            repository.updateStep(1, "2024-01-15", 10, 5, 75)
        }

        assertNotNull(ex)
    }

    // ─── removeStep ───────────────────────────────────────────────────────────

    @Test
    fun `removeStep - returns updated list on success`() = runTest {
        coEvery { api.removeStepFromDailyPlan(any()) } returns Response.success(listOf(planDto))

        val result = repository.removeStep(dailyPlanStepId = 1)

        assertEquals(1, result.size)
        coVerify(exactly = 1) { api.removeStepFromDailyPlan(1) }
    }

    @Test
    fun `removeStep - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.removeStepFromDailyPlan(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.removeStep(1) }

        assertNotNull(ex)
    }

    @Test
    fun `removeStep - throws AppError NetworkError on IOException`() = runTest {
        coEvery { api.removeStepFromDailyPlan(any()) } throws IOException("connection lost")

        val ex = catchError<AppError.NetworkError> { repository.removeStep(1) }

        assertNotNull(ex)
        assertTrue(ex!!.error is IOException)
    }

    // ─── copyDayPlan ──────────────────────────────────────────────────────────

    @Test
    fun `copyDayPlan - returns copied list on success`() = runTest {
        coEvery { api.copyDailyPlan(any()) } returns Response.success(listOf(planDto))

        val result = repository.copyDayPlan(fromDate = "2024-01-14")

        assertEquals(1, result.size)
        coVerify(exactly = 1) { api.copyDailyPlan(DailyPlanCopyDto(fromDate = "2024-01-14")) }
    }

    @Test
    fun `copyDayPlan - throws IllegalArgumentException when response body is null`() = runTest {
        coEvery { api.copyDailyPlan(any()) } returns Response.success(null)

        val ex = catchError<IllegalArgumentException> { repository.copyDayPlan("2024-01-14") }

        assertNotNull(ex)
    }

    @Test
    fun `copyDayPlan - throws AppError ApiError on HTTP 404`() = runTest {
        coEvery { api.copyDailyPlan(any()) } returns makeErrorResponse(404)

        val ex = catchError<AppError.ApiError> { repository.copyDayPlan("2024-01-14") }

        assertNotNull(ex)
        assertEquals(404, ex!!.status)
        assertEquals("error_api_404", ex.uiCode)
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun makeErrorResponse(code: Int, body: String = ""): Response<List<DayPlanDto>> =
        Response.error(code, body.toResponseBody("application/json".toMediaType()))

    private suspend inline fun <reified T : Throwable> catchError(
        crossinline block: suspend () -> Unit
    ): T? = try {
        block()
        null
    } catch (e: Throwable) {
        e as? T
    }
}
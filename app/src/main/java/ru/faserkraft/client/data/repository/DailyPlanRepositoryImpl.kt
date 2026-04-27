package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.api.DailyPlanApi
import ru.faserkraft.client.data.api.DailyPlanCopyDto
import ru.faserkraft.client.data.api.DailyPlanStepCreateDto
import ru.faserkraft.client.data.api.DailyPlanStepUpdateDto
import ru.faserkraft.client.data.dto.toDomain
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import ru.faserkraft.client.error.AppError
import ru.faserkraft.client.utils.callApi
import javax.inject.Inject

/**
 * Реализация DailyPlanRepository
 */
class DailyPlanRepositoryImpl @Inject constructor(
    private val dailyPlanApi: DailyPlanApi
) : DailyPlanRepository {

    override suspend fun getDayPlans(date: String): Result<List<DailyPlan>> {
        return try {
            val response = dailyPlanApi.getDayPlans(date)
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun addStepToDailyPlan(
        date: String,
        employeeId: Int,
        stepId: Int,
        plannedQuantity: Int
    ): Result<List<DailyPlan>> {
        return try {
            val request = DailyPlanStepCreateDto(date, employeeId, stepId, plannedQuantity)
            val response = dailyPlanApi.addStepToDailyPlan(request)
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun updateStepInDailyPlan(
        stepId: Int,
        date: String,
        plannedQuantity: Int
    ): Result<List<DailyPlan>> {
        return try {
            // TODO: Нужно получить текущие данные шага для обновления
            val request = DailyPlanStepUpdateDto(
                step_id = stepId,
                plan_date = date,
                step_definition_id = 0, // TODO: Получить из текущего состояния
                employee_id = 0,       // TODO: Получить из текущего состояния
                planned_quantity = plannedQuantity
            )
            val response = dailyPlanApi.updateStepInDailyPlan(stepId, request)
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun removeStepFromDailyPlan(dailyPlanStepId: Int): Result<List<DailyPlan>> {
        return try {
            val response = dailyPlanApi.removeStepFromDailyPlan(dailyPlanStepId)
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }

    override suspend fun copyDailyPlan(
        fromDate: String,
        toDate: String
    ): Result<List<DailyPlan>> {
        return try {
            val request = DailyPlanCopyDto(fromDate, toDate)
            val response = dailyPlanApi.copyDailyPlan(request)
            callApi(response).map { list -> list.map { it.toDomain() } }
        } catch (e: Exception) {
            Result.failure(AppError.fromException(e))
        }
    }
}
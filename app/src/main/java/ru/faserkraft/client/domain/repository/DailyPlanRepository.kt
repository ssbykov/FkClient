package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.DailyPlan

/**
 * Repository interface для работы с дневными планами
 */
interface DailyPlanRepository {
    /**
     * Получить планы на день
     */
    suspend fun getDayPlans(date: String): Result<List<DailyPlan>>

    /**
     * Добавить шаг в дневной план
     */
    suspend fun addStepToDailyPlan(
        date: String,
        employeeId: Int,
        stepId: Int,
        plannedQuantity: Int
    ): Result<List<DailyPlan>>

    /**
     * Обновить шаг в дневном плане
     */
    suspend fun updateStepInDailyPlan(
        stepId: Int,
        date: String,
        plannedQuantity: Int
    ): Result<List<DailyPlan>>

    /**
     * Удалить шаг из дневного плана
     */
    suspend fun removeStepFromDailyPlan(dailyPlanStepId: Int): Result<List<DailyPlan>>

    /**
     * Скопировать план с одного дня на другой
     */
    suspend fun copyDailyPlan(
        fromDate: String,
        toDate: String
    ): Result<List<DailyPlan>>
}


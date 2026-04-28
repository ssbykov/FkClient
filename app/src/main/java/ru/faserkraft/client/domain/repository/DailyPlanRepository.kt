package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.DailyPlan

interface DailyPlanRepository {
    suspend fun getDayPlans(date: String): List<DailyPlan>
    suspend fun addStep(
        planDate: String,
        employeeId: Int,
        stepId: Int,
        plannedQuantity: Int,
    ): List<DailyPlan>
    suspend fun updateStep(
        stepId: Int,
        planDate: String,
        stepDefinitionId: Int,
        employeeId: Int,
        plannedQuantity: Int,
    ): List<DailyPlan>
    suspend fun removeStep(dailyPlanStepId: Int): List<DailyPlan>
    suspend fun copyDayPlan(fromDate: String): List<DailyPlan>
}
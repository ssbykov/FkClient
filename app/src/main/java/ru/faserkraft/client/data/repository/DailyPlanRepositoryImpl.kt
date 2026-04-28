package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import ru.faserkraft.client.dto.DailyPlanCopyDto
import ru.faserkraft.client.dto.DailyPlanStepCreateDto
import ru.faserkraft.client.dto.DailyPlanStepUpdateDto
import ru.faserkraft.client.repository.ApiRepository
import javax.inject.Inject

class DailyPlanRepositoryImpl @Inject constructor(
    private val apiRepository: ApiRepository,
) : DailyPlanRepository {

    override suspend fun getDayPlans(date: String): List<DailyPlan> =
        apiRepository.getDayPlans(date).orEmpty().map { it.toDomain() }

    override suspend fun addStep(
        planDate: String,
        employeeId: Int,
        stepId: Int,
        plannedQuantity: Int,
    ): List<DailyPlan> =
        requireNotNull(
            apiRepository.addStepToDailyPlan(
                DailyPlanStepCreateDto(
                    planDate = planDate,
                    stepId = stepId,
                    employeeId = employeeId,
                    plannedQuantity = plannedQuantity,
                )
            )
        ).map { it.toDomain() }

    override suspend fun updateStep(
        stepId: Int,
        planDate: String,
        stepDefinitionId: Int,
        employeeId: Int,
        plannedQuantity: Int,
    ): List<DailyPlan> =
        requireNotNull(
            apiRepository.updateStepInDailyPlan(
                DailyPlanStepUpdateDto(
                    stepId = stepId,
                    planDate = planDate,
                    stepDefinitionId = stepDefinitionId,
                    employeeId = employeeId,
                    plannedQuantity = plannedQuantity,
                )
            )
        ).map { it.toDomain() }

    override suspend fun removeStep(dailyPlanStepId: Int): List<DailyPlan> =
        requireNotNull(apiRepository.removeStepFromDailyPlan(dailyPlanStepId))
            .map { it.toDomain() }

    override suspend fun copyDayPlan(fromDate: String): List<DailyPlan> =
        requireNotNull(apiRepository.copyDailyPlan(DailyPlanCopyDto(fromDate)))
            .map { it.toDomain() }
}
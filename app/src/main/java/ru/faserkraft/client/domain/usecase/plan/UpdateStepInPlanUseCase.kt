package ru.faserkraft.client.domain.usecase.plan

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import javax.inject.Inject

class UpdateStepInPlanUseCase @Inject constructor(
    private val repository: DailyPlanRepository
) {
    suspend operator fun invoke(
        stepId: Int,
        planDate: String,
        stepDefinitionId: Int,
        employeeId: Int,
        plannedQuantity: Int,
    ): List<DailyPlan> =
        repository.updateStep(stepId, planDate, stepDefinitionId, employeeId, plannedQuantity)
}
package ru.faserkraft.client.domain.usecase.plan

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import javax.inject.Inject

class AddStepToPlanUseCase @Inject constructor(
    private val repository: DailyPlanRepository
) {
    suspend operator fun invoke(
        planDate: String,
        employeeId: Int,
        stepId: Int,
        plannedQuantity: Int,
    ): List<DailyPlan> = repository.addStep(planDate, employeeId, stepId, plannedQuantity)
}
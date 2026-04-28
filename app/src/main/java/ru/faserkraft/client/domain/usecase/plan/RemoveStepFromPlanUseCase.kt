package ru.faserkraft.client.domain.usecase.plan

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import javax.inject.Inject

class RemoveStepFromPlanUseCase @Inject constructor(
    private val repository: DailyPlanRepository
) {
    suspend operator fun invoke(dailyPlanStepId: Int): List<DailyPlan> =
        repository.removeStep(dailyPlanStepId)
}
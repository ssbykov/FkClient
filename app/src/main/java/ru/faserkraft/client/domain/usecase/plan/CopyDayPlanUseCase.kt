package ru.faserkraft.client.domain.usecase.plan

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import javax.inject.Inject

class CopyDayPlanUseCase @Inject constructor(
    private val repository: DailyPlanRepository
) {
    suspend operator fun invoke(fromDate: String): List<DailyPlan> =
        repository.copyDayPlan(fromDate)
}
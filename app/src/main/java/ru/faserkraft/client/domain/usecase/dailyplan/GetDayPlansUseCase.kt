package ru.faserkraft.client.domain.usecase.dailyplan

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.repository.DailyPlanRepository
import javax.inject.Inject

/**
 * Use Case для получения дневных планов
 */
class GetDayPlansUseCase @Inject constructor(
    private val dailyPlanRepository: DailyPlanRepository
) {
    suspend operator fun invoke(date: String): Result<List<DailyPlan>> {
        return dailyPlanRepository.getDayPlans(date)
    }
}


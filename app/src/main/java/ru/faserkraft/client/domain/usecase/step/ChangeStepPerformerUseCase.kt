package ru.faserkraft.client.domain.usecase.step

import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.StepRepository
import javax.inject.Inject

class ChangeStepPerformerUseCase @Inject constructor(
    private val repository: StepRepository
) {
    suspend operator fun invoke(stepId: Int, newEmployeeId: Int): Product =
        repository.changeStepPerformer(stepId, newEmployeeId)
}
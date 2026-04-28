package ru.faserkraft.client.data.repository

import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.StepRepository
import ru.faserkraft.client.repository.ApiRepository
import javax.inject.Inject

class StepRepositoryImpl @Inject constructor(
    private val apiRepository: ApiRepository,
) : StepRepository {

    override suspend fun closeStep(stepId: Int): Product =
        requireNotNull(apiRepository.postStep(stepId)).toDomain()

    override suspend fun changeStepPerformer(stepId: Int, newEmployeeId: Int): Product =
        requireNotNull(apiRepository.changeStepPerformer(stepId, newEmployeeId)).toDomain()
}
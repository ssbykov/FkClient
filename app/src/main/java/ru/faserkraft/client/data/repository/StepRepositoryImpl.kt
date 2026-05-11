package ru.faserkraft.client.data.repository


import ru.faserkraft.client.data.mapper.toDomain
import ru.faserkraft.client.data.network.Api
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.domain.repository.StepRepository
import ru.faserkraft.client.utils.logger.Logger
import javax.inject.Inject

class StepRepositoryImpl @Inject constructor(
    private val api: Api,
    logger: Logger,
) : BaseRepository(logger), StepRepository {

    override suspend fun closeStep(stepId: Int): Product =
        requireNotNull(callApi { api.postStep(stepId) }).toDomain()

    override suspend fun changeStepPerformer(stepId: Int, newEmployeeId: Int): Product =
        requireNotNull(callApi { api.changeStepPerformer(stepId, newEmployeeId) }).toDomain()
}
package ru.faserkraft.client.domain.repository

import ru.faserkraft.client.domain.model.Product

interface StepRepository {
    suspend fun closeStep(stepId: Int): Product
    suspend fun changeStepPerformer(stepId: Int, newEmployeeId: Int): Product
}
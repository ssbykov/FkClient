package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.data.dto.PeriodStatisticsDto
import ru.faserkraft.client.data.dto.ProcessCountStatDto
import ru.faserkraft.client.data.dto.StepCountStatDto
import ru.faserkraft.client.domain.model.PeriodStatistics
import ru.faserkraft.client.domain.model.ProcessCountStat
import ru.faserkraft.client.domain.model.StepCountStat

fun StepCountStatDto.toDomain(): StepCountStat {
    return StepCountStat(
        processId = this.processId,
        processName = this.processName,
        stepDefinitionId = this.stepDefinitionId,
        order = this.order,
        stepName = this.stepName,
        employeeId = this.employeeId,
        employeeName = this.employeeName,
        count = this.count
    )
}

fun ProcessCountStatDto.toDomain() = ProcessCountStat(
    processId = processId,
    processName = processName,
    count = count
)

fun PeriodStatisticsDto.toDomain(): PeriodStatistics {
    return PeriodStatistics(
        finishedProducts = this.finishedProducts.map { it.toDomain() },
        totalSteps = this.totalSteps.map { it.toDomain() }
    )
}
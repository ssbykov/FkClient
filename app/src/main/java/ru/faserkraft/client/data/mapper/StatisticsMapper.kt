package ru.faserkraft.client.data.mapper

import ru.faserkraft.client.data.dto.EmployeeEarningsDto
import ru.faserkraft.client.data.dto.EmployeePlanStatDto
import ru.faserkraft.client.data.dto.PeriodStatisticsDto
import ru.faserkraft.client.data.dto.ProcessCountStatDto
import ru.faserkraft.client.data.dto.StepCountStatDto
import ru.faserkraft.client.domain.model.EmployeeEarningsStat
import ru.faserkraft.client.domain.model.EmployeePlanStat
import ru.faserkraft.client.domain.model.PeriodStatistics
import ru.faserkraft.client.domain.model.ProcessCountStat
import ru.faserkraft.client.domain.model.StepCountStat
import java.math.BigDecimal

fun StepCountStatDto.toDomain() = StepCountStat(
    processId = processId,
    processName = processName,
    sizeTypeId = sizeTypeId,
    sizeTypeName = sizeTypeName,
    stepDefinitionId = stepDefinitionId,
    order = order,
    templateId = templateId,
    stepName = stepName,
    employeeId = employeeId,
    employeeName = employeeName,
    count = count,
    totalAmount = BigDecimal(totalAmount)
)


fun ProcessCountStatDto.toDomain() = ProcessCountStat(
    processId = processId,
    processName = processName,
    count = count
)

fun EmployeePlanStatDto.toDomain() = EmployeePlanStat(
    employeeId = employeeId,
    employeeName = employeeName,
    workingDays = workingDays,
    steps = steps.map { it.toDomain() }
)

fun EmployeeEarningsDto.toDomain(): EmployeeEarningsStat {
    return EmployeeEarningsStat(
        employeeId = employeeId,
        employeeName = employeeName,
        totalEarned = BigDecimal(totalEarned),
        steps = steps.map { it.toDomain() }
    )
}

fun PeriodStatisticsDto.toDomain(): PeriodStatistics {
    return PeriodStatistics(
        totalWorkingDays = totalWorkingDays,
        finishedProducts = finishedProducts.map { it.toDomain() },
        totalSteps = totalSteps.map { it.toDomain() },
        employeePlans = employeePlans.map { it.toDomain() },
        employeeEarnings = employeeEarnings.map { it.toDomain() },
        firstHalfEarnings = firstHalfEarnings.map { it.toDomain() },
        totalEarnedAll = totalEarnedAll.toBigDecimal(),
    )
}
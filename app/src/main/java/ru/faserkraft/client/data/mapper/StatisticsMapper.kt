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

fun StepCountStatDto.toDomain(): StepCountStat {
    return StepCountStat(
        processId = this.processId,
        processName = this.processName,
        sizeTypeId = this.sizeTypeId,
        sizeTypeName = this.sizeTypeName,
        stepDefinitionId = this.stepDefinitionId,
        order = this.order,
        stepName = this.stepName,
        employeeId = this.employeeId,
        employeeName = this.employeeName,
        count = this.count,
        totalAmount = BigDecimal(this.totalAmount)
    )
}

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
        employeeId = this.employeeId,
        employeeName = this.employeeName,
        totalEarned = BigDecimal(this.totalEarned),
        steps = this.steps.map { it.toDomain() }
    )
}

fun PeriodStatisticsDto.toDomain(): PeriodStatistics {
    return PeriodStatistics(
        totalWorkingDays = this.totalWorkingDays,
        finishedProducts = this.finishedProducts.map { it.toDomain() },
        totalSteps = this.totalSteps.map { it.toDomain() },
        employeePlans = this.employeePlans.map { it.toDomain() },
        employeeEarnings = this.employeeEarnings.map { it.toDomain() },
        totalEarnedAll = BigDecimal(this.totalEarnedAll)
    )
}
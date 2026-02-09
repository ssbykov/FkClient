package ru.faserkraft.client.dto


data class EmployeePlanDto(
    val id: Int,
    val employee: EmployeeDto,
    val stepDefinition: StepDefinitionDto,
    val workProcess: String,
    val plannedQuantity: Int,
    val actualQuantity: Int
)


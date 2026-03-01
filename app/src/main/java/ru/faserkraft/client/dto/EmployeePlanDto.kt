package ru.faserkraft.client.dto

import java.io.Serializable


data class EmployeePlanDto(
    val id: Int,
    val date: String,
    val employee: EmployeeDto,
    val stepDefinition: StepDefinitionDto,
    val workProcess: String,
    val plannedQuantity: Int,
    val actualQuantity: Int
): Serializable


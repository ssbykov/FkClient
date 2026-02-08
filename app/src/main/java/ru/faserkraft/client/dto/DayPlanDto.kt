package ru.faserkraft.client.dto

import com.google.gson.annotations.SerializedName

data class DayPlanDto(
    @SerializedName("employee_id")
    val employeeId: Int,
    val date: String,
    val id: Int,
    val employee: EmployeeDto,
    val steps: List<DayPlanStepDto>
)

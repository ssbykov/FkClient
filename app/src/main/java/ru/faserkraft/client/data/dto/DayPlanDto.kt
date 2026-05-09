package ru.faserkraft.client.data.dto

import com.google.gson.annotations.SerializedName

data class DayPlanDto(
    @SerializedName("employee_id")
    val employeeId: Int,
    val date: String,
    val id: Int,
    val employee: EmployeeDto,
    val steps: List<DayPlanStepDto>
)

data class DailyPlanCopyDto(
    @SerializedName("from_date")
    val fromDate: String,
)
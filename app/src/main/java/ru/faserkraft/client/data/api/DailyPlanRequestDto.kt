package ru.faserkraft.client.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.dto.*

/**
 * DTO для создания шага в дневном плане
 */
data class DailyPlanStepCreateDto(
    val plan_date: String,
    val employee_id: Int,
    val step_id: Int,
    val planned_quantity: Int
)

/**
 * DTO для обновления шага в дневном плане
 */
data class DailyPlanStepUpdateDto(
    val step_id: Int,
    val plan_date: String,
    val step_definition_id: Int,
    val employee_id: Int,
    val planned_quantity: Int
)

/**
 * DTO для копирования дневного плана
 */
data class DailyPlanCopyDto(
    val from_date: String,
    val to_date: String
)

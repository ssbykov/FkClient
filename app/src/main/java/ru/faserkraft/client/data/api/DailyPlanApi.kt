package ru.faserkraft.client.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import ru.faserkraft.client.BuildConfig
import ru.faserkraft.client.data.dto.*

/**
 * API интерфейс для работы с дневными планами
 */
interface DailyPlanApi {

    @GET(BuildConfig.BASE_URL + "daily-plans")
    suspend fun getDayPlans(@Query("date") date: String): Response<List<DailyPlanDto>>

    @POST(BuildConfig.BASE_URL + "daily-plans/steps")
    suspend fun addStepToDailyPlan(@Body step: DailyPlanStepCreateDto): Response<List<DailyPlanDto>>

    @PUT(BuildConfig.BASE_URL + "daily-plans/steps/{id}")
    suspend fun updateStepInDailyPlan(
        @Path("id") stepId: Int,
        @Body step: DailyPlanStepUpdateDto
    ): Response<List<DailyPlanDto>>

    @DELETE(BuildConfig.BASE_URL + "daily-plans/steps/{id}")
    suspend fun removeStepFromDailyPlan(@Path("id") dailyPlanStepId: Int): Response<List<DailyPlanDto>>

    @POST(BuildConfig.BASE_URL + "daily-plans/copy")
    suspend fun copyDailyPlan(@Body copyRequest: DailyPlanCopyDto): Response<List<DailyPlanDto>>
}


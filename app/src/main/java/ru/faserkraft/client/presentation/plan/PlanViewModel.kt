package ru.faserkraft.client.presentation.plan

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.auth.AppAuth
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.domain.usecase.plan.AddStepToPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.CopyDayPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.GetDayPlansUseCase
import ru.faserkraft.client.domain.usecase.plan.RemoveStepFromPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.UpdateStepInPlanUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStepEmployeeDayUseCase
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.utils.apiPattern
import ru.faserkraft.client.utils.getToday
import ru.faserkraft.client.utils.isPlanDateEditable
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class PlanViewModel @Inject constructor(
    private val getDayPlansUseCase: GetDayPlansUseCase,
    private val addStepToPlanUseCase: AddStepToPlanUseCase,
    private val updateStepInPlanUseCase: UpdateStepInPlanUseCase,
    private val removeStepFromPlanUseCase: RemoveStepFromPlanUseCase,
    private val copyDayPlanUseCase: CopyDayPlanUseCase,
    private val getEmployeesUseCase: GetEmployeesUseCase,
    private val getProcessesUseCase: GetProcessesUseCase,
    private val getProductsByLastStepUseCase: GetProductsByLastStepUseCase,
    private val getProductsByStepEmployeeDayUseCase: GetProductsByStepEmployeeDayUseCase,
    private val appAuth: AppAuth,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState

    private val _events = MutableSharedFlow<PlanEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PlanEvent> = _events

    init {
        loadUserRole()
    }

    // ---------- Пользователь / роль ----------

    private fun loadUserRole() {
        val role = appAuth.getRegistrationData()?.role
        _uiState.update { it.copy(userRole = role) }
        recomputeCanEdit()
    }

    // ---------- Планы ----------

    fun loadPlans(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            recomputeCanEdit(date)
            runCatching { getDayPlansUseCase(date) }
                .onSuccess { plans ->
                    _uiState.update { it.copy(plans = plans) }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun addStepToPlan(
        planDate: String,
        employeeId: Int,
        stepId: Int,
        plannedQuantity: Int,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { addStepToPlanUseCase(planDate, employeeId, stepId, plannedQuantity) }
                .onSuccess { plans -> _uiState.update { it.copy(plans = plans) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun updateStepInPlan(
        stepId: Int,
        planDate: String,
        stepDefinitionId: Int,
        employeeId: Int,
        plannedQuantity: Int,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching {
                updateStepInPlanUseCase(
                    stepId,
                    planDate,
                    stepDefinitionId,
                    employeeId,
                    plannedQuantity,
                )
            }
                .onSuccess { plans -> _uiState.update { it.copy(plans = plans) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun removeStepFromPlan(dailyPlanStepId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { removeStepFromPlanUseCase(dailyPlanStepId) }
                .onSuccess { plans -> _uiState.update { it.copy(plans = plans) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun copyDayPlan(fromDate: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { copyDayPlanUseCase(fromDate) }
                .onSuccess { plans ->
                    val today = getToday()
                    recomputeCanEdit(today)
                    _uiState.update { it.copy(plans = plans) }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Сотрудники и процессы ----------

    fun loadEmployees() {
        viewModelScope.launch {
            runCatching { getEmployeesUseCase() }
                .onSuccess { employees ->
                    _uiState.update { it.copy(employees = employees) }
                }
                .onFailure { emitError(it) }
        }
    }

    fun loadProcesses() {
        viewModelScope.launch {
            runCatching { getProcessesUseCase() }
                .onSuccess { processes ->
                    _uiState.update { it.copy(processes = processes) }
                }
                .onFailure { emitError(it) }
        }
    }

    // ---------- Продукты ----------

    fun loadProductsByLastStep(processId: Int, stepDefinitionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, filteredProducts = emptyList()) }
            runCatching { getProductsByLastStepUseCase(processId, stepDefinitionId) }
                .onSuccess { products ->
                    _uiState.update { it.copy(filteredProducts = products) }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun loadProductsByStepEmployeeDay(
        stepDefinitionId: Int,
        day: String,
        employeeId: Int,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, filteredProducts = emptyList()) }
            runCatching {
                getProductsByStepEmployeeDayUseCase(stepDefinitionId, day, employeeId)
            }
                .onSuccess { products ->
                    _uiState.update { it.copy(filteredProducts = products) }
                }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Навигационные хелперы ----------

    @RequiresApi(Build.VERSION_CODES.O)
    fun shiftDate(days: Long) {
        val current = _uiState.value.date
        if (!apiPattern.matches(current)) return
        val newDate = LocalDate.parse(current).plusDays(days).toString()
        loadPlans(newDate) // loadPlans вызывает recomputeCanEdit внутри
    }

    fun selectPlanStep(plan: DailyPlan, step: DailyPlanStep) {
        _uiState.update { it.copy(selectedPlan = plan, selectedStep = step) }
    }

    fun clearSelectedPlanStep() {
        _uiState.update { it.copy(selectedPlan = null, selectedStep = null) }
    }

    // ---------- canEdit / isPastDate ----------

    fun recomputeCanEdit(dateApi: String? = null) {
        val effectiveDate = dateApi ?: _uiState.value.date
        val isMaster = _uiState.value.userRole == UserRole.MASTER
        val isPast = !isPlanDateEditable(effectiveDate)
        _uiState.update {
            it.copy(
                date = effectiveDate,
                isPastDate = isPast,
                canEdit = isMaster,
            )
        }
    }

    // ---------- Ошибки ----------

    private suspend fun emitError(e: Throwable) {
        _events.emit(PlanEvent.ShowError(e.message ?: UNKNOWN_ERROR))
    }

    companion object {
        private const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}
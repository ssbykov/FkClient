package ru.faserkraft.client.presentation.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.usecase.employee.GetEmployeesUseCase
import ru.faserkraft.client.domain.usecase.plan.AddStepToPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.CopyDayPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.GetDayPlansUseCase
import ru.faserkraft.client.domain.usecase.plan.RemoveStepFromPlanUseCase
import ru.faserkraft.client.domain.usecase.plan.UpdateStepInPlanUseCase
import ru.faserkraft.client.domain.usecase.process.GetProcessesUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByLastStepUseCase
import ru.faserkraft.client.domain.usecase.product.GetProductsByStepEmployeeDayUseCase
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
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanUiState())
    val uiState: StateFlow<PlanUiState> = _uiState

    private val _events = MutableSharedFlow<PlanEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PlanEvent> = _events

    // ---------- Планы ----------

    fun loadPlans(date: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, date = date) }
            runCatching { getDayPlansUseCase(date) }
                .onSuccess { _uiState.update { state -> state.copy(plans = it) } }
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
                .onSuccess { _uiState.update { state -> state.copy(plans = it) } }
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
                .onSuccess { _uiState.update { state -> state.copy(plans = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun removeStepFromPlan(dailyPlanStepId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { removeStepFromPlanUseCase(dailyPlanStepId) }
                .onSuccess { _uiState.update { state -> state.copy(plans = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun copyDayPlan(fromDate: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            runCatching { copyDayPlanUseCase(fromDate) }
                .onSuccess { _uiState.update { state -> state.copy(plans = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    // ---------- Сотрудники и процессы ----------

    fun loadEmployees() {
        viewModelScope.launch {
            runCatching { getEmployeesUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(employees = it) } }
                .onFailure { emitError(it) }
        }
    }

    fun loadProcesses() {
        viewModelScope.launch {
            runCatching { getProcessesUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(processes = it) } }
                .onFailure { emitError(it) }
        }
    }

    // ---------- Фильтрация продуктов ----------

    fun loadProductsByLastStep(processId: Int, stepDefinitionId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, filteredProducts = emptyList()) }
            runCatching { getProductsByLastStepUseCase(processId, stepDefinitionId) }
                .onSuccess { _uiState.update { state -> state.copy(filteredProducts = it) } }
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
                .onSuccess { _uiState.update { state -> state.copy(filteredProducts = it) } }
                .onFailure { emitError(it) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ---------- Вспомогательное ----------

    private suspend fun emitError(e: Throwable) {
        _events.emit(PlanEvent.ShowError(e.message ?: UNKNOWN_ERROR))
    }

    companion object {
        private const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
}
package ru.faserkraft.client.ui.dailyplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.domain.usecase.dailyplan.GetDayPlansUseCase
import javax.inject.Inject

/**
 * ViewModel для работы с дневными планами
 */
@HiltViewModel
class DailyPlanViewModel @Inject constructor(
    private val getDayPlansUseCase: GetDayPlansUseCase
) : ViewModel() {

    private val _dayPlansState = MutableStateFlow<UiState<List<DailyPlan>>>(UiState.Idle)
    val dayPlansState: StateFlow<UiState<List<DailyPlan>>> = _dayPlansState

    /**
     * Получить планы на день
     */
    fun getDayPlans(date: String) {
        viewModelScope.launch {
            _dayPlansState.value = UiState.Loading
            getDayPlansUseCase(date)
                .onSuccess { plans ->
                    _dayPlansState.value = UiState.Success(plans)
                }
                .onFailure { exception ->
                    _dayPlansState.value = UiState.Error(exception)
                }
        }
    }

    /**
     * Сбросить планы
     */
    fun clearDayPlans() {
        _dayPlansState.value = UiState.Idle
    }
}


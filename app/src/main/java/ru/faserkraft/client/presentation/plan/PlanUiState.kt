package ru.faserkraft.client.presentation.plan

import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.Employee
import ru.faserkraft.client.domain.model.Process
import ru.faserkraft.client.domain.model.Product
import ru.faserkraft.client.utils.getToday

data class PlanUiState(
    val isLoading: Boolean = false,
    val isActionInProgress: Boolean = false,
    val date: String = getToday(),
    val plans: List<DailyPlan> = emptyList(),
    val employees: List<Employee> = emptyList(),
    val processes: List<Process> = emptyList(),
    val filteredProducts: List<Product> = emptyList(),
)
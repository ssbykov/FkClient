package ru.faserkraft.client.presentation.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentStatisticsBinding

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()
    private lateinit var totalProcessAdapter: StatTotalProcessAdapter
    private lateinit var stepsProcessAdapter: StatProcessStepsAdapter
    private lateinit var employeeAdapter: EmployeeStatAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerViews() {
        totalProcessAdapter = StatTotalProcessAdapter()
        binding.rvTotalByProcess.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = totalProcessAdapter
            itemAnimator = null
        }

        stepsProcessAdapter = StatProcessStepsAdapter()
        binding.rvStepsByProcess.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stepsProcessAdapter
            itemAnimator = null
        }

        employeeAdapter = EmployeeStatAdapter { employee ->
            // Задел под переход на экран детальной статистики сотрудника
            // val action = StatisticsFragmentDirections.actionStatisticsToEmployeeDetail(
            //     employeeId = employee.employeeId,
            //     employeeName = employee.employeeName,
            //     dateFrom = viewModel.currentDateFrom,
            //     dateTo = viewModel.currentDateTo
            // )
            // findNavController().navigate(action)
        }
        binding.rvEmployees.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = employeeAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        binding.apply {
            toggleGroupMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                val mode = when (checkedId) {
                    btnModeEmployees.id -> StatMode.BY_EMPLOYEE
                    else -> StatMode.BY_PROCESS
                }
                viewModel.setMode(mode)
            }

            chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
                if (viewModel.uiState.value.isLoading) return@setOnCheckedStateChangeListener
                val period = when (checkedIds.firstOrNull()) {
                    chipQuarter.id -> StatPeriod.QUARTER
                    chipYear.id -> StatPeriod.YEAR
                    else -> StatPeriod.MONTH
                }
                viewModel.setPeriod(period)
            }

            btnPrevPeriod.setOnClickListener {
                if (viewModel.uiState.value.isLoading) return@setOnClickListener
                viewModel.shiftPeriod(-1)
            }

            btnNextPeriod.setOnClickListener {
                if (viewModel.uiState.value.isLoading) return@setOnClickListener
                viewModel.shiftPeriod(1)
            }

            swipeRefresh.setOnRefreshListener {
                viewModel.refresh()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collectLatest { state ->
                        renderState(state)
                    }
                }

                launch {
                    viewModel.events.collect { event ->
                        if (_binding == null || !isAdded) return@collect
                        when (event) {
                            is StatisticsEvent.ShowError -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: StatisticsUiState) {
        val b = _binding ?: return
        b.apply {
            val isSwipeRefreshing = swipeRefresh.isRefreshing
            if (isSwipeRefreshing && !state.isLoading) {
                swipeRefresh.isRefreshing = false
            }

            progressBar.isVisible = state.isLoading && !isSwipeRefreshing
            btnPrevPeriod.isEnabled = !state.isLoading
            btnNextPeriod.isEnabled = !state.isLoading
            chipGroupPeriod.isEnabled = !state.isLoading
            chipMonth.isEnabled = !state.isLoading
            chipQuarter.isEnabled = !state.isLoading
            chipYear.isEnabled = !state.isLoading
            toggleGroupMode.isEnabled = !state.isLoading

            // Период
            tvPeriodLabel.text = state.periodLabel

            // Переключение видимости блоков режимов
            val isProcessMode = state.mode == StatMode.BY_PROCESS
            containerProcesses.isVisible = isProcessMode
            rvEmployees.isVisible = !isProcessMode

            if (isProcessMode) {
                // Карточка 1 (Итого): скрываем целиком, если список пуст
                cardTotal.isVisible = state.totalByProcess.isNotEmpty()
                if (state.totalByProcess.isNotEmpty()) {
                    totalProcessAdapter.submitList(state.totalByProcess)
                    val grandTotal = state.totalByProcess.sumOf { it.completedProducts }
                    tvGrandTotal.text = getString(R.string.grand_total_format, grandTotal)
                } else {
                    totalProcessAdapter.submitList(emptyList())
                }

                // Карточка 2 (Этапы): скрываем, если этапов нет
                cardSteps.isVisible = state.stepsByProcess.isNotEmpty()
                if (state.stepsByProcess.isNotEmpty()) {
                    tvStepsWorkingDays.text =
                        getString(R.string.stat_working_days_format, state.periodWorkingDays)
                    stepsProcessAdapter.submitList(state.stepsByProcess)
                } else {
                    stepsProcessAdapter.submitList(emptyList())
                }
            } else {
                employeeAdapter.submitList(state.employees)
            }
        }
    }

    override fun onDestroyView() {
        binding.rvTotalByProcess.adapter = null
        binding.rvStepsByProcess.adapter = null
        binding.rvEmployees.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
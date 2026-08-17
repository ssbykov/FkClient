package ru.faserkraft.client.presentation.plan

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
        // Адаптер для верхней карточки (Итого по процессам)
        totalProcessAdapter = StatTotalProcessAdapter()
        binding.rvTotalByProcess.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = totalProcessAdapter
            itemAnimator = null
        }

        // Адаптер для нижней карточки (Этапы по процессам)
        stepsProcessAdapter = StatProcessStepsAdapter()
        binding.rvStepsByProcess.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stepsProcessAdapter
            itemAnimator = null
        }
    }

    private fun setupListeners() {
        binding.apply {
            chipGroupPeriod.setOnCheckedStateChangeListener { _, checkedIds ->
                val period = when (checkedIds.firstOrNull()) {
                    chipQuarter.id -> StatPeriod.QUARTER
                    chipYear.id -> StatPeriod.YEAR
                    else -> StatPeriod.MONTH
                }
                viewModel.setPeriod(period)
            }

            btnPrevPeriod.setOnClickListener {
                viewModel.shiftPeriod(-1)
            }

            btnNextPeriod.setOnClickListener {
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
                        when (event) {
                            is StatisticsEvent.ShowError -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderState(state: StatisticsUiState) {
        binding.apply {
            // Карточка 1: Общее количество
            totalProcessAdapter.submitList(state.totalByProcess)
            val grandTotal = state.totalByProcess.sumOf { it.completedProducts }
            tvGrandTotal.text = getString(R.string.grand_total_format, grandTotal)

            // Карточка 2: Этапы
            stepsProcessAdapter.submitList(state.stepsByProcess)

            // Скрываем вторую карточку, если этапов нет (чтобы не висела пустая)
            cardSteps.visibility = if (state.stepsByProcess.isEmpty()) View.GONE else View.VISIBLE

            // Период и загрузка
            tvPeriodLabel.text = state.periodLabel
            swipeRefresh.isRefreshing = state.isLoading
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvTotalByProcess.adapter = null
        binding.rvStepsByProcess.adapter = null
        _binding = null
    }
}
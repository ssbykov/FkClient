package ru.faserkraft.client.presentation.plan

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentDayPlanBinding
import ru.faserkraft.client.domain.model.DailyPlan
import ru.faserkraft.client.domain.model.DailyPlanStep
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.utils.converter.convertDate
import ru.faserkraft.client.utils.converter.formatPlanDate
import ru.faserkraft.client.utils.converter.getToday
import ru.faserkraft.client.utils.ext.navigateSafely
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class DayPlanFragment : Fragment() {

    private val viewModel: PlanViewModel by activityViewModels()

    private var _binding: FragmentDayPlanBinding? = null
    private val binding get() = _binding!!

    private val plansAdapter = PlansAdapter(
        onEditPlanClick = { plan, step -> onEditPlan(plan, step) },
        onEmployeeProductsClick = { plan, step -> onEmployeeProducts(plan, step) },
    )

    private var datePicker: MaterialDatePicker<Long>? = null
    private var activeDialog: AlertDialog? = null

    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    // ---------- Lifecycle ----------

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupSwipeToDelete()
        setupDateControls()
        setupSwipeRefresh()
        setupFab()
        observeState()
        observeEvents()

        // Берём ранее выбранную дату из ViewModel или сегодняшнюю при первом старте
        val currentDate = viewModel.uiState.value.date.ifEmpty { getToday() }
        viewModel.recomputeCanEdit(currentDate)
        viewModel.loadPlans(currentDate)
        viewModel.loadEmployees()
        viewModel.loadProcesses()
    }

    override fun onDestroyView() {
        if (::emptyObserver.isInitialized) {
            plansAdapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvPlans.adapter = null
        datePicker?.dismiss()
        datePicker = null
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Setup ----------

    private fun setupRecycler() {
        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        plansAdapter.registerAdapterDataObserver(emptyObserver)

        binding.rvPlans.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = plansAdapter
        }
    }

    private fun setupSwipeToDelete() {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder,
            ) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                val state = viewModel.uiState.value
                if (!state.canEdit || state.isPastDate || state.isLoading) return 0
                val item = plansAdapter.currentList.getOrNull(vh.bindingAdapterPosition)
                return if (item is EmployeePlanUiItem.Header) 0
                else super.getSwipeDirs(rv, vh)
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                val item = plansAdapter.currentList.getOrNull(position)

                plansAdapter.notifyItemChanged(position)

                if (item !is EmployeePlanUiItem.Step || !isAdded) return

                showConfirmDialog(
                    title = "Удалить план?",
                    message = "Вы уверены, что хотите удалить этот шаг плана?"
                ) {
                    viewModel.removeStepFromPlan(item.step.id)
                }
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvPlans)
    }

    private fun setupDateControls() {
        binding.btnPrevDate.setOnClickListener {
            if (viewModel.uiState.value.isLoading) return@setOnClickListener
            viewModel.shiftDate(-1)
        }
        binding.btnNextDate.setOnClickListener {
            if (viewModel.uiState.value.isLoading) return@setOnClickListener
            viewModel.shiftDate(+1)
        }

        binding.etDate.setOnClickListener {
            if (viewModel.uiState.value.isLoading) return@setOnClickListener
            it.clearFocus()
            showDatePicker()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            val date = viewModel.uiState.value.date.ifEmpty { getToday() }
            viewModel.loadPlans(date)
        }
    }

    private fun setupFab() {
        binding.fabAddPlan.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.isLoading) return@setOnClickListener
            if (state.isPastDate) showCopyPlanDialog() else openAddPlanScreen()
        }
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            val isSwipeRefreshing = b.swipeRefresh.isRefreshing
            if (isSwipeRefreshing && !state.isLoading) {
                b.swipeRefresh.isRefreshing = false
            }

            // Индикатор загрузки и блокировка контролов
            b.progressBar.isVisible = state.isLoading && !isSwipeRefreshing
            b.btnPrevDate.isEnabled = !state.isLoading
            b.btnNextDate.isEnabled = !state.isLoading
            b.etDate.isEnabled = !state.isLoading
            b.fabAddPlan.isEnabled = !state.isLoading

            // Дата
            if (state.date.isNotEmpty()) {
                b.etDate.setText(convertDate(state.date))
            }

            // FAB видимость и иконка
            if (state.canEdit) {
                b.fabAddPlan.isVisible = true
                if (state.isPastDate) {
                    b.fabAddPlan.setImageResource(R.drawable.ic_copy)
                    b.fabAddPlan.contentDescription = getString(R.string.copy_plan)
                } else {
                    b.fabAddPlan.setImageResource(R.drawable.ic_add)
                    b.fabAddPlan.contentDescription = getString(R.string.add_plan)
                }
            } else {
                b.fabAddPlan.isVisible = false
            }

            plansAdapter.setCanEdit(state.canEdit && !state.isPastDate)
            plansAdapter.submitPlans(state.plans)

            checkEmpty()
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            if (_binding == null || !isAdded) return@collectFlow
            when (event) {
                is PlanEvent.ShowError -> showErrorSnackbar(event.message)
            }
        }
    }

    // ---------- Helpers ----------

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = plansAdapter.itemCount == 0 && !b.progressBar.isVisible
        b.tvEmptyPlans.isVisible = isEmpty
        b.rvPlans.isVisible = !isEmpty
    }

    // ---------- DatePicker ----------

    private fun showDatePicker() {
        if (datePicker?.isAdded == true) return

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выбор даты")
            .build()
            .also { datePicker = it }

        picker.addOnPositiveButtonClickListener { utcMillis ->
            val (apiDate, _) = formatPlanDate(utcMillis)
            viewModel.recomputeCanEdit(apiDate)
            viewModel.loadPlans(apiDate)
        }
        picker.addOnDismissListener { datePicker = null }
        picker.show(parentFragmentManager, "day_plan_date_picker")
    }

    // ---------- Navigation ----------

    private fun openAddPlanScreen() {
        viewModel.loadEmployees()
        viewModel.loadProcesses()
        requireParentFragment().findNavController()
            .navigateSafely(R.id.action_workbenchContainerFragment_to_addDayPlanFragment)
    }

    private fun onEditPlan(plan: DailyPlan, step: DailyPlanStep) {
        viewModel.selectPlanStep(plan, step)
        viewModel.loadEmployees()
        viewModel.loadProcesses()
        requireParentFragment().findNavController()
            .navigateSafely(R.id.action_workbenchContainerFragment_to_addDayPlanFragment)
    }

    private fun onEmployeeProducts(plan: DailyPlan, step: DailyPlanStep) {
        viewModel.selectPlanStep(plan, step)
        requireParentFragment().findNavController()
            .navigateSafely(R.id.action_workbenchContainerFragment_to_employeePlanProductsFragment)
    }

    // ---------- Copy plan dialog ----------

    private fun showCopyPlanDialog() {
        showConfirmDialog(
            title = "Скопировать план?",
            message = "План будет скопирован на текущую дату."
        ) {
            val sourceDate = viewModel.uiState.value.date
            viewModel.copyDayPlan(sourceDate)
            val today = getToday()
            viewModel.recomputeCanEdit(today)
            viewModel.loadPlans(today)
        }
    }

    // ---------- Dialog ----------

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да") { d, _ ->
                onConfirm()
                d.dismiss()
                activeDialog = null
            }
            .setNegativeButton("Отмена") { d, _ ->
                d.dismiss()
                activeDialog = null
            }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }
}
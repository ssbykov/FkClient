package ru.faserkraft.client.presentation.plan

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.utils.formatPlanDate
import ru.faserkraft.client.utils.getToday

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupSwipeToDelete()
        setupDateControls()
        setupSwipeRefresh()
        setupFab()
        observeState()
        observeEvents()

        if (savedInstanceState == null) {
            viewModel.loadPlans(getToday())
            viewModel.loadEmployees()
            viewModel.loadProcesses()
        }
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
                if (!state.canEdit || state.isPastDate) return 0
                val item = plansAdapter.currentList.getOrNull(vh.bindingAdapterPosition)
                return if (item is EmployeePlanUiItem.Header) 0
                else super.getSwipeDirs(rv, vh)
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                val item = plansAdapter.currentList.getOrNull(position)
                // Визуально откатываем — удаляем только после подтверждения
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDateControls() {
        binding.btnPrevDate.setOnClickListener { viewModel.shiftDate(-1) }
        binding.btnNextDate.setOnClickListener { viewModel.shiftDate(+1) }

        binding.etDate.setOnClickListener {
            it.clearFocus()
            showDatePicker()
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPlans(viewModel.uiState.value.date)
        }
    }

    private fun setupFab() {
        binding.fabAddPlan.setOnClickListener {
            val state = viewModel.uiState.value
            if (state.isPastDate) showCopyPlanDialog() else openAddPlanScreen()
        }
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            // Загрузка
            b.swipeRefresh.isRefreshing = state.isLoading
            b.swipeRefresh.isEnabled = !state.isLoading
            b.etDate.isEnabled = !state.isLoading
            b.fabAddPlan.isEnabled = !state.isLoading

            // Дата
            b.etDate.setText(convertDate(state.date)) // api → ui формат

            // FAB видимость и иконка
            if (state.canEdit) {
                b.fabAddPlan.visibility = View.VISIBLE
                if (state.isPastDate) {
                    b.fabAddPlan.setImageResource(R.drawable.ic_copy)
                    b.fabAddPlan.contentDescription = getString(R.string.copy_plan)
                } else {
                    b.fabAddPlan.setImageResource(R.drawable.ic_add)
                    b.fabAddPlan.contentDescription = getString(R.string.add_plan)
                }
            } else {
                b.fabAddPlan.visibility = View.GONE
            }

            // canEdit в адаптер: только мастер + не прошедший день
            plansAdapter.setCanEdit(state.canEdit && !state.isPastDate)

            // Список
            plansAdapter.submitPlans(state.plans)
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is PlanEvent.ShowError -> showDialog(event.message)
            }
        }
    }

    // ---------- Empty state ----------

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = plansAdapter.itemCount == 0
        b.tvEmptyPlans.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvPlans.visibility = if (isEmpty) View.GONE else View.VISIBLE
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

    // ---------- Навигация ----------

    private fun openAddPlanScreen() {
        viewModel.loadEmployees()
        viewModel.loadProcesses()
        findNavController().navigate(
            R.id.action_dayPlanFragment_to_addDayPlanFragment
        )
    }

    private fun onEditPlan(plan: DailyPlan, step: DailyPlanStep) {
        viewModel.selectPlanStep(plan, step)
        viewModel.loadEmployees()
        viewModel.loadProcesses()
        findNavController().navigate(R.id.action_dayPlanFragment_to_addDayPlanFragment)
    }

    private fun onEmployeeProducts(plan: DailyPlan, step: DailyPlanStep) {
        viewModel.selectPlanStep(plan, step)
        findNavController().navigate(
            R.id.action_dayPlanFragment_to_employeePlanProductsFragment
        )
    }

    // ---------- Copy plan dialog ----------

    private fun showCopyPlanDialog() {
        showConfirmDialog(
            title = "Скопировать план?",
            message = "План будет скопирован на текущую дату."
        ) {
            val sourceDate = viewModel.uiState.value.date
            viewModel.copyDayPlan(sourceDate)
            // После копирования переходим на сегодня
            val today = getToday()
            viewModel.recomputeCanEdit(today)
            viewModel.loadPlans(today)
        }
    }

    // ---------- Dialogs ----------

    private fun showDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { d, _ -> d.dismiss(); activeDialog = null }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Да") { d, _ -> onConfirm(); d.dismiss(); activeDialog = null }
            .setNegativeButton("Отмена") { d, _ -> d.dismiss(); activeDialog = null }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    // ---------- Lifecycle ----------

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
}
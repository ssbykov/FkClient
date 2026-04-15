package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.EmployeePlanUiItem
import ru.faserkraft.client.adapter.PlansAdapter
import ru.faserkraft.client.databinding.FragmentDayPlanBinding
import ru.faserkraft.client.dto.DailyPlanCopyDto
import ru.faserkraft.client.dto.EmployeePlanDto
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.utils.apiPattern
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.utils.formatPlanDate
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.time.LocalDate

class DayPlanFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentDayPlanBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PlansAdapter
    private var currentUserRole: UserRole? = null
    private var canEdit: Boolean = false
    private var isPastDate: Boolean = false
    private var datePicker: MaterialDatePicker<Long>? = null

    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etDate.setOnClickListener {
            it.clearFocus()
            showDatePicker()
        }

        adapter = PlansAdapter(
            canEdit,
            onEditPlanClick = { employeePlan ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.setEmployees()
                    viewModel.setProcesses()
                    val bundle = bundleOf("plan" to employeePlan)
                    findNavController().navigate(
                        R.id.action_dayPlanFragment_to_addDayPlanFragment,
                        bundle
                    )
                }
            },
            onEmployeeProductsClick = { employeePlan ->
                val action = DayPlanFragmentDirections
                    .actionDayPlanFragmentToEmployeePlanProductsFragment(employeePlan)
                findNavController().navigate(action)
            }
        )

        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlans.adapter = adapter

        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
            override fun onMove(
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                t: RecyclerView.ViewHolder
            ) = false

            override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int {
                if (!canEdit || isPastDate) return 0

                val position = vh.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position)
                return if (item is EmployeePlanUiItem.Header) 0
                else super.getSwipeDirs(rv, vh)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position)

                // Сразу откатываем визуально — удалять будем только после подтверждения
                adapter.notifyItemChanged(position)

                if (item !is EmployeePlanUiItem.Step) return

                if (!isAdded) return

                AlertDialog.Builder(requireContext())
                    .setTitle("Удалить план?")
                    .setMessage("Вы уверены, что хотите удалить этот шаг плана?")
                    .setPositiveButton("Да") { dialog, _ ->
                        dialog.dismiss()
                        val planId = item.plan.id

                        viewLifecycleOwner.lifecycleScope.launch {
                            val result = viewModel.removeStepFromDailyPlan(planId)
                            result.onSuccess {
                                // Обновление придёт через dayPlans observer автоматически
                            }.onFailure {
                                // Ошибка уйдёт в errorState — список не трогаем
                            }
                        }
                    }
                    .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvPlans)

        viewModel.dayPlans.observe(viewLifecycleOwner) { dayPlans ->
            val planDate = dayPlans.date
            val planDateUi = convertDate(dayPlans.date)
            binding.etDate.setText(planDateUi)

            if (dayPlans.plans.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                recomputeCanEdit(planDateApi = null)
                return@observe
            }

            recomputeCanEdit(planDate)

            val uiItems = mutableListOf<EmployeePlanUiItem>()
            dayPlans.plans.forEach { plan ->
                val steps = plan.steps
                if (steps.isEmpty()) return@forEach
                uiItems += EmployeePlanUiItem.Header(plan.employee.name)
                steps.forEach { step ->
                    val employeePlan = EmployeePlanDto(
                        id = step.id,
                        date = planDate,
                        employee = plan.employee,
                        stepDefinition = step.stepDefinition,
                        workProcess = step.workProcess,
                        plannedQuantity = step.plannedQuantity,
                        actualQuantity = step.actualQuantity
                    )
                    uiItems += EmployeePlanUiItem.Step(employeePlan)
                }
            }
            adapter.submitList(uiItems)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val isLoading = state.isLoading
            binding.swipeRefresh.isRefreshing = isLoading
            binding.swipeRefresh.isEnabled = !isLoading
            binding.etDate.isEnabled = !isLoading
            binding.fabAddPlan.isEnabled = !isLoading
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    if (!isAdded) return@collect
                    AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            val planDate = convertDate(binding.etDate.text.toString())
            viewModel.getDayPlans(planDate)
        }

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            currentUserRole = user?.role
            recomputeCanEdit()
        }

        binding.fabAddPlan.setOnClickListener {
            if (currentUserRole != UserRole.MASTER) return@setOnClickListener
            if (isPastDate) showCopyPlanDialog() else openAddPlanScreen()
        }

        binding.btnPrevDate.setOnClickListener { shiftDate(-1) }
        binding.btnNextDate.setOnClickListener { shiftDate(1) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        binding.rvPlans.adapter = null
        datePicker?.dismiss()
        datePicker = null
        _binding = null
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyPlans.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvPlans.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showDatePicker() {
        // уже есть и показан – выходим
        datePicker?.let { picker ->
            if (picker.isAdded) return
        }

        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выбор даты")

        val picker = builder.build()
        datePicker = picker

        picker.addOnPositiveButtonClickListener { utcMillis ->
            val (datePlan, etDatePlan) = formatPlanDate(utcMillis)
            binding.etDate.setText(etDatePlan)
            viewModel.getDayPlans(datePlan)
        }

        picker.addOnDismissListener {
            datePicker = null
        }

        picker.show(parentFragmentManager, "day_plan_date_picker")
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun openAddPlanScreen() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.setEmployees()
            viewModel.setProcesses()

            val action =
                DayPlanFragmentDirections
                    .actionDayPlanFragmentToAddDayPlanFragment(
                        plan = null,
                        planDate = binding.etDate.text?.toString().orEmpty()
                    )

            findNavController().navigate(action)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showCopyPlanDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Скопировать план?")
            .setMessage("План будет скопирован на текущую дату.")
            .setPositiveButton("Да") { dialog, _ ->
                dialog.dismiss()

                val sourcePlanDate = convertDate(binding.etDate.text?.toString().orEmpty())

                viewLifecycleOwner.lifecycleScope.launch {
                    val result = viewModel.copyDailyPlan(DailyPlanCopyDto(sourcePlanDate))

                    result.onSuccess {
                        val todayApiDate = LocalDate.now().toString()
                        val todayUiDate = convertDate(todayApiDate)

                        binding.etDate.setText(todayUiDate)
                        recomputeCanEdit(todayApiDate)
                        viewModel.getDayPlans(todayApiDate)
                    }.onFailure {
                        // Ошибка уже попадёт в errorState
                    }
                }
            }
            .setNegativeButton("Отмена") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun recomputeCanEdit(planDateApi: String? = null) {
        val isMaster = currentUserRole == UserRole.MASTER

        val effectiveDateApi = planDateApi ?: run {
            val text = binding.etDate.text?.toString().orEmpty()
            val api = convertDate(text)
            api.takeIf { apiPattern.matches(it) }
        }

        val selectedDate = effectiveDateApi?.let { LocalDate.parse(it) }
        val today = LocalDate.now()

        isPastDate = selectedDate?.isBefore(today) == true

        canEdit = isMaster
        binding.fabAddPlan.visibility = if (canEdit) View.VISIBLE else View.GONE

        if (canEdit) {
            if (isPastDate) {
                binding.fabAddPlan.setImageResource(R.drawable.ic_copy)
                binding.fabAddPlan.contentDescription = "Скопировать план"
            } else {
                binding.fabAddPlan.setImageResource(R.drawable.ic_add)
                binding.fabAddPlan.contentDescription = "Добавить план"
            }
        }

        if (this::adapter.isInitialized) {
            val allowStepEdit = isMaster && !isPastDate
            adapter.setCanEdit(allowStepEdit)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun shiftDate(days: Long) {
        val currentText = binding.etDate.text?.toString().orEmpty()
        val apiDate = convertDate(currentText)

        if (!apiPattern.matches(apiDate)) return

        val newDate = LocalDate.parse(apiDate).plusDays(days)
        val newApiDate = newDate.toString()
        val newUiDate = convertDate(newApiDate)

        binding.etDate.setText(newUiDate)
        recomputeCanEdit(newApiDate)
        viewModel.getDayPlans(newApiDate)
    }

}
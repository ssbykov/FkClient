package ru.faserkraft.client.activity

import android.annotation.SuppressLint
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
import ru.faserkraft.client.dto.EmployeePlanDto
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DayPlanFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentDayPlanBinding
    private var currentUserRole: UserRole? = null
    private var canEdit: Boolean = false

    @SuppressLint("ConstantLocale")
    companion object {
        private val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val uiFormat  = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

        private val apiPattern = Regex("""\d{4}-\d{2}-\d{2}""")   // yyyy-MM-dd
        private val uiPattern  = Regex("""\d{2}\.\d{2}\.\d{4}""") // dd.MM.yyyy

        fun convertDate(dateStr: String): String {
            return when {
                apiPattern.matches(dateStr) -> {
                    uiFormat.format(apiFormat.parse(dateStr)!!)
                }
                uiPattern.matches(dateStr) -> {
                    apiFormat.format(uiFormat.parse(dateStr)!!)
                }
                else -> dateStr
            }
        }

        fun isPlanDateEditable(planDateApi: String): Boolean {
            val planDate = apiFormat.parse(planDateApi) ?: return false
            val today = apiFormat.parse(apiFormat.format(Date()))!!
            // true, если дата плана сегодня или в будущем
            return !planDate.before(today)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.etDate.setOnClickListener {
            it.clearFocus()
            showDatePicker()
        }

        val adapter = PlansAdapter(

            onStepClick = { employeePlan ->
                if (!canEdit) return@PlansAdapter

                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.setEmployees()
                    viewModel.setProcesses()

                    val bundle = bundleOf("plan" to employeePlan)
                    findNavController().navigate(
                        R.id.action_dayPlanFragment_to_addDayPlanFragment,
                        bundle
                    )
                }
            }
        )

        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlans.adapter = adapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.END
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                if (!canEdit) return 0

                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position)
                return if (item is EmployeePlanUiItem.Header) 0
                else super.getSwipeDirs(recyclerView, viewHolder)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position)

                adapter.notifyItemChanged(position)

                if (item !is EmployeePlanUiItem.Step) return

                AlertDialog.Builder(requireContext())
                    .setTitle("Удалить план?")
                    .setMessage("Вы уверены, что хотите удалить этот шаг плана?")
                    .setPositiveButton("Да") { dialog, _ ->
                        val planId = item.plan.id

                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.removeStepFromDailyPlan(planId)
                        }

                        val mutable = adapter.currentList.toMutableList()
                        mutable.removeAt(position)
                        adapter.submitList(mutable)
                        dialog.dismiss()
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvPlans)

        viewModel.dayPlans.observe(viewLifecycleOwner) { plans ->

            if (plans.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                updateCanEditForCurrentState(planDateApi = null)
                return@observe
            }

            val firstPlan = plans.first()

            val planDateUi = convertDate(firstPlan.date)
            binding.etDate.setText(planDateUi)

            updateCanEditForCurrentState(firstPlan.date)

            val uiItems = mutableListOf<EmployeePlanUiItem>()

            plans.forEach { plan ->
                val steps = plan.steps
                if (steps.isEmpty()) return@forEach

                uiItems += EmployeePlanUiItem.Header(plan.employee.name)

                steps.forEach { step ->
                    val employeePlan = EmployeePlanDto(
                        id = step.id,
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
            binding.swipeRefresh.isRefreshing = state.isLoading
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
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
            updateCanEditForCurrentState()
        }

        binding.fabAddPlan.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.setEmployees()
                viewModel.setProcesses()
                val action =
                    DayPlanFragmentDirections
                        .actionDayPlanFragmentToAddDayPlanFragment()
                findNavController().navigate(action)
            }
        }
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выбор даты")

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            val (datePlan, etDatePlan) = formatPlanDate(utcMillis)
            binding.etDate.setText(etDatePlan)
            viewModel.getDayPlans(datePlan)
        }

        picker.show(parentFragmentManager, "day_plan_date_picker")
    }

    private fun formatPlanDate(timeMillis: Long): Pair<String, String> {
        val date = Date(timeMillis)
        val apiDate = apiFormat.format(date)
        val uiDate = uiFormat.format(date)
        return apiDate to uiDate
    }

    private fun updateCanEditForCurrentState(planDateApi: String? = null) {
        val isMaster = currentUserRole == UserRole.MASTER

        val effectiveDateApi = planDateApi ?: run {
            val text = binding.etDate.text?.toString().orEmpty()
            val api = convertDate(text)
            api.takeIf { apiPattern.matches(it) }
        }

        val isDateEditable = effectiveDateApi?.let { isPlanDateEditable(it) } ?: false

        canEdit = isMaster && isDateEditable
        binding.fabAddPlan.visibility = if (canEdit) View.VISIBLE else View.GONE
    }

}

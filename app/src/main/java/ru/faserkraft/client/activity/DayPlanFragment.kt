package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.EmployeePlanUiItem
import ru.faserkraft.client.adapter.PlansAdapter
import ru.faserkraft.client.databinding.FragmentDayPlanBinding
import ru.faserkraft.client.dto.EmployeePlanDto
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayPlanFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentDayPlanBinding

    private var canSwipe: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDayPlanBinding.inflate(inflater, container, false)
        getPlansToday()
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PlansAdapter()
        binding.rvPlans.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlans.adapter = adapter

        val swipeCallback = object : ItemTouchHelper.SimpleCallback(
            0,
            ItemTouchHelper.END   // свайп только вправо
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
                if (!canSwipe) return 0

                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position)
                // Запрещаем свайпать заголовки
                return if (item is EmployeePlanUiItem.Header) 0 else super.getSwipeDirs(
                    recyclerView,
                    viewHolder
                )
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.currentList.getOrNull(position)

                // Сразу откатываем визуальный свайп
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
                        // Ничего не делаем, элемент уже восстановлен notifyItemChanged
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.rvPlans)

        viewModel.dayPlans.observe(viewLifecycleOwner) { plans ->
            plans ?: return@observe

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
            getPlansToday()
        }

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            binding.fabAddPlan.visibility =
                if (user?.role == UserRole.MASTER) View.VISIBLE else View.GONE

            canSwipe = (user?.role == UserRole.MASTER)
        }

        binding.fabAddPlan.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.setEmployees()
                viewModel.setProcesses()
                findNavController().navigate(R.id.action_dayPlanFragment_to_addDayPlanFragment)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPlansToday() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(formatter)
        viewModel.getDayPlans(today)
    }

}
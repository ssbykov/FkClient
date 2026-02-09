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
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.EmployeePlanUiItem
import ru.faserkraft.client.adapter.PlansAdapter
import ru.faserkraft.client.databinding.FragmentDayPlanBinding
import ru.faserkraft.client.dto.EmployeePlanDto
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayPlanFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentDayPlanBinding


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

        viewModel.dayPlans.observe(viewLifecycleOwner) { plans ->
            plans ?: return@observe

            val uiItems = mutableListOf<EmployeePlanUiItem>()

            plans.forEach { plan ->
                // хедер сотрудника
                uiItems += EmployeePlanUiItem.Header(plan.employee.name)

                // конвертация шагов в EmployeePlanDto для карточки
                plan.steps.forEach { step ->
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


        binding.btnRefresh.setOnClickListener {
            getPlansToday()
        }


    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPlansToday() {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val today = LocalDate.now().format(formatter)
        viewModel.getDayPlans(today)
    }

}
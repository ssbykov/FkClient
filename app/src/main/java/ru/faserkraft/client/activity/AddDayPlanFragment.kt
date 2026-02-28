package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.AddStepsAdapter
import ru.faserkraft.client.adapter.EmployeeUi
import ru.faserkraft.client.adapter.EmployeesAdapter
import ru.faserkraft.client.adapter.ProcessAdapter
import ru.faserkraft.client.adapter.ProcessUi
import ru.faserkraft.client.adapter.StepUi
import ru.faserkraft.client.databinding.FragmentAddDayPlanBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AddDayPlanFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentAddDayPlanBinding


    private lateinit var employeesAdapter: EmployeesAdapter
    private lateinit var processAdapter: ProcessAdapter
    private lateinit var stepsAdapter: AddStepsAdapter

    private var employees: List<EmployeeUi> = emptyList()
    private var processes: List<ProcessUi> = emptyList()
    private var steps: List<StepUi> = emptyList()

    private var selectedEmployeeIndex: Int? = null
    private var selectedProcessIndex: Int? = null
    private var selectedStepIndex: Int? = null

    // дату храним в millis (UTC) и отображаем форматированную строку
    private var selectedDateUtc: Long? = null
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddDayPlanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Адаптеры
        employeesAdapter = EmployeesAdapter(requireContext())
        processAdapter = ProcessAdapter(requireContext())
        stepsAdapter = AddStepsAdapter(requireContext())

        binding.actvEmployee.setAdapter(employeesAdapter)
        binding.actvProcess.setAdapter(processAdapter)
        binding.actvStep.setAdapter(stepsAdapter)

        // Загрузка списков из VM
        viewModel.employees.observe(viewLifecycleOwner) { list ->
            employees = list?.map { EmployeeUi(it.id, it.name) }.orEmpty()
            employeesAdapter.setItems(employees)
        }


        viewModel.processes.observe(viewLifecycleOwner) { list ->
            processes = list
                ?.map { ProcessUi(it.id, it.name) }
                .orEmpty()
            processAdapter.setItems(processes)
        }


        // выбор сотрудника
        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
            selectedEmployeeIndex = position
        }

        // выбор процесса
        binding.actvProcess.setOnItemClickListener { _, _, position, _ ->
            selectedProcessIndex = position

            val processDto = viewModel.processes.value
                ?.getOrNull(position)          // исходный ProcessDto
                ?: return@setOnItemClickListener

            steps = processDto.steps
                .map { stepDto ->
                    StepUi(
                        id = stepDto.id,
                        name = stepDto.template.name
                    )
                }

            stepsAdapter.setItems(steps)
        }

        // выбор этапа
        binding.actvStep.setOnItemClickListener { _, _, position, _ ->
            selectedStepIndex = position
        }

        // выбор даты через MaterialDatePicker
        binding.etDate.setOnClickListener {
            it.clearFocus()
            showDatePicker()
        }

        // обработка ошибок (как у тебя во всех фрагментах)
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

        // кнопка "Создать план"
        binding.btnCreatePlan.setOnClickListener {
            onCreatePlanClicked()
        }

        // uiState: включение/выключение кнопки и прогресс
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.btnCreatePlan.isEnabled = !state.isActionInProgress
            binding.progressCreatePlan.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
        }
    }

    private fun showDatePicker() {
        val builder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выбор даты")

        // по умолчанию сегодняшняя
        selectedDateUtc?.let {
            builder.setSelection(it)
        }

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { utcMillis ->
            selectedDateUtc = utcMillis
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
            }
            // если бэку нужно "yyyy-MM-dd", поменяй форматтер
            binding.etDate.setText(dateFormat.format(calendar.time))
        }

        picker.show(parentFragmentManager, "day_plan_date_picker")
    }

    private fun onCreatePlanClicked() {
        val empIndex = selectedEmployeeIndex
        val procIndex = selectedProcessIndex
        val stepIndex = selectedStepIndex
        val dateMillis = selectedDateUtc
        val qtyText = binding.etQty.text?.toString()?.trim()

        // Простейшая валидация
        if (empIndex == null || empIndex !in employees.indices) {
            showMessage("Сотрудник не выбран")
            return
        }
        if (dateMillis == null) {
            showMessage("Дата не выбрана")
            return
        }
        if (procIndex == null || procIndex !in processes.indices) {
            showMessage("Процесс не выбран")
            return
        }
        if (stepIndex == null || stepIndex !in steps.indices) {
            showMessage("Этап не выбран")
            return
        }
        val plannedQuantity = qtyText?.toIntOrNull()
        if (plannedQuantity == null || plannedQuantity <= 0) {
            showMessage("Укажите корректное количество")
            return
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Europe/Moscow")
        }
        val planDate = dateFormat.format(dateMillis)
        val employeeId = employees[empIndex].id
        val stepId = steps[stepIndex].id

        // тут вызываешь метод VM, который ты реализуешь под свой API
        viewLifecycleOwner.lifecycleScope.launch {
            val result = viewModel.addStepToDailyPlan(
                planDate = planDate,
                employeeId = employeeId,
                stepId = stepId,
                plannedQuantity = plannedQuantity
            )

            result.onSuccess {
                findNavController().navigateUp()
            }.onFailure {
                // ошибка уйдет в errorState
            }
        }
    }

    private fun showMessage(text: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(text)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .show()
    }

}

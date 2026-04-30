//package ru.faserkraft.client.activity
//
//import android.app.AlertDialog
//import android.os.Bundle
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.activityViewModels
//import androidx.lifecycle.Lifecycle
//import androidx.lifecycle.lifecycleScope
//import androidx.lifecycle.repeatOnLifecycle
//import androidx.navigation.fragment.findNavController
//import androidx.navigation.fragment.navArgs
//import kotlinx.coroutines.launch
//import ru.faserkraft.client.adapter.EmployeeUi
//import ru.faserkraft.client.adapter.EmployeesAdapter
//import ru.faserkraft.client.databinding.FragmentEditStepBinding
//import ru.faserkraft.client.dto.ProductDto
//import ru.faserkraft.client.viewmodel.ScannerViewModel
//
//
//class EditStepFragment_OLD : Fragment() {
//
//    private val viewModel: ScannerViewModel by activityViewModels()
//    private val args: EditStepFragmentArgs by navArgs()
//
//    private var _binding: FragmentEditStepBinding? = null
//    private val binding get() = _binding!!
//
//    private lateinit var employeesAdapter: EmployeesAdapter
//    private var employees: List<EmployeeUi> = emptyList()
//    private var product: ProductDto? = null
//    private var isEmployeesReady = false
//    private var initialEmployeeId: Int? = null
//    private var selectedIndex: Int? = null
//
//    private var activeDialog: AlertDialog? = null
//
//    override fun onCreateView(
//        inflater: LayoutInflater, container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View {
//        _binding = FragmentEditStepBinding.inflate(inflater, container, false)
//        return binding.root
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//        employeesAdapter = EmployeesAdapter(requireContext())
//        binding.actvEmployee.setAdapter(employeesAdapter)
//
//        initialEmployeeId = args.step.performedBy?.id
//
//        viewModel.employees.observe(viewLifecycleOwner) { list ->
//            employees = list?.map { EmployeeUi(it.id, it.name) }.orEmpty()
//            employeesAdapter.setItems(employees)
//            isEmployeesReady = true
//
//            if (selectedIndex == null) {
//                selectEmployeeIfPossible()
//            }
//        }
//
//        viewModel.productState.observe(viewLifecycleOwner) { state ->
//            val b = _binding ?: return@observe
//            b.tvSerial.text = state?.serialNumber ?: "Не определен"
//            b.tvStep.text = args.step.stepDefinition.template.name
//            product = state
//            if (selectedIndex == null) {
//                selectEmployeeIfPossible()
//            }
//        }
//
//        binding.actvEmployee.setOnItemClickListener { _, _, position, _ ->
//            selectedIndex = position
//        }
//
//        viewLifecycleOwner.lifecycleScope.launch {
//            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
//                viewModel.errorState.collect { msg ->
//                    activeDialog?.dismiss()
//                    activeDialog = AlertDialog.Builder(requireContext())
//                        .setMessage(msg)
//                        .setPositiveButton("ОК") { dialog, _ ->
//                            viewModel.resetIsHandled()
//                            dialog.dismiss()
//                            activeDialog = null
//                        }
//                        .also { it.setOnDismissListener { activeDialog = null } }
//                        .show()
//                }
//            }
//        }
//
//        binding.btnEdit.setOnClickListener {
//            val index = selectedIndex
//
//            if (index == null) {
//                findNavController().navigateUp()
//                return@setOnClickListener
//            }
//
//            if (index < 0 || index >= employees.size) {
//                showMessage("Сотрудник не выбран")
//                return@setOnClickListener
//            }
//
//            viewLifecycleOwner.lifecycleScope.launch {
//                val step = args.step
//                val newEmployee = employees.getOrNull(index) ?: return@launch
//
//                if (newEmployee.id == initialEmployeeId) {
//                    findNavController().navigateUp()
//                    return@launch
//                }
//
//                val result = viewModel.changeStepPerformer(
//                    step = step,
//                    newEmployeeId = newEmployee.id
//                )
//
//                if (_binding == null) return@launch
//
//                result.onSuccess {
//                    findNavController().navigateUp()
//                }
//                // onFailure → errorState
//            }
//        }
//
//        viewModel.uiState.observe(viewLifecycleOwner) { state ->
//            val b = _binding ?: return@observe
//            b.btnEdit.isEnabled = !state.isActionInProgress
//            b.progressEdit.visibility =
//                if (state.isActionInProgress) View.VISIBLE else View.GONE
//        }
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        activeDialog?.dismiss()
//        activeDialog = null
//        _binding = null
//    }
//
//    private fun selectEmployeeIfPossible() {
//        val id = args.step.performedBy?.id
//        if (!isEmployeesReady || id == null) return
//        val selectedEmployee = employees.find { it.id == id }
//        selectedEmployee?.let {
//            _binding?.actvEmployee?.setText(it.name, false)
//        }
//    }
//
//    private fun showMessage(text: String) {
//        activeDialog?.dismiss()
//        activeDialog = AlertDialog.Builder(requireContext())
//            .setMessage(text)
//            .setPositiveButton("ОК") { dialog, _ ->
//                dialog.dismiss()
//                activeDialog = null
//            }
//            .also { it.setOnDismissListener { activeDialog = null } }
//            .show()
//    }
//}
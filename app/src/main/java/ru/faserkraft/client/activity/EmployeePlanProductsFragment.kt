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
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.ProductsInventoryByProcessAdapter
import ru.faserkraft.client.adapter.ProductsInventoryByProcessUiItem
import ru.faserkraft.client.databinding.FragmentEmployeePlanProductsBinding
import ru.faserkraft.client.utils.convertDate
import ru.faserkraft.client.viewmodel.ScannerViewModel

class EmployeePlanProductsFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private val args: EmployeePlanProductsFragmentArgs by navArgs()

    private var _binding: FragmentEmployeePlanProductsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsInventoryByProcessAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEmployeePlanProductsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        val planDate = convertDate(args.employeePlanDto.date)
        binding.tvDetailTitle.text = getString(
            R.string.employee_plan_products,
            planDate
        )

        adapter = ProductsInventoryByProcessAdapter {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.handleProductQr(it)
                findNavController().navigate(
                    R.id.action_employeePlanProductsFragment_to_productFullFragment
                )
            }
        }

        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupObservers()
        renderHeader()
        loadData()
        setupRefresh()
    }

    private fun setupRecycler() {
        binding.rvProductsDetail.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsDetail.adapter = adapter
    }

    private fun setupObservers() {
        // список продуктов, подтверждающих выполнение плана
        viewModel.productsInventoryByProcess.observe(viewLifecycleOwner) { list ->
            val items = list.orEmpty().map {
                ProductsInventoryByProcessUiItem(
                    id = it.id,
                    serialNumber = it.serialNumber,
                    createdAt = it.createdAt
                )
            }
            adapter.submitList(items)
        }

        // состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            binding.swipeRefreshDetail.isRefreshing = state.isLoading
            binding.swipeRefreshDetail.isEnabled = !state.isLoading
        }

        // ошибки
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
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

    }

    private fun renderHeader() {
        // args.employeePlanDto (пример имени аргумента — подставь своё)
        with(args.employeePlanDto) {
            binding.tvProcessName.text = workProcess
            binding.tvEmployeeName.text = employee.name
            binding.tvStageName.text = stepDefinition.template.name
        }
    }

    private fun loadData() {
        // запрос продуктов по плану сотрудника (подставь свой метод/аргументы)
        viewLifecycleOwner.lifecycleScope.launch {
            with(args.employeePlanDto) {
                viewModel.getProductsByStepEmployeeDay(
                    stepDefinitionId = stepDefinition.id,
                    day =date,
                    employeeId = employee.id
                )
            }
        }
    }

    private fun setupRefresh() {
        binding.swipeRefreshDetail.setOnRefreshListener {
            loadData()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

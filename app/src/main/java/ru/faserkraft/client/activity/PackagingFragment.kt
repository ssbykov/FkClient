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
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.PackagingContentAdapter
import ru.faserkraft.client.adapter.PackagingContentUiItem
import ru.faserkraft.client.databinding.FragmentPackagingBinding
import ru.faserkraft.client.domain.model.Packaging
import ru.faserkraft.client.domain.model.UiState
import ru.faserkraft.client.model.UserData
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.ui.base.BaseFragment
import ru.faserkraft.client.ui.common.SharedUiViewModel
import ru.faserkraft.client.ui.packaging.PackagingViewModel
import ru.faserkraft.client.utils.collectIn
import ru.faserkraft.client.utils.collectEventsIn
import ru.faserkraft.client.utils.formatIsoToUi
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId


/**
 * МИГРИРОВАННЫЙ PackagingFragment с новой архитектурой
 *
 * НОВОЕ: Использует PackagingViewModel вместо ScannerViewModel
 * НОВОЕ: Использует StateFlow вместо LiveData
 * НОВОЕ: Работает с Domain Models вместо DTOs
 * НОВОЕ: Наследуется от BaseFragment для общей логики
 */
@AndroidEntryPoint
class PackagingFragment : BaseFragment<PackagingViewModel>() {

    override val viewModel: PackagingViewModel by activityViewModels()
    private val sharedUiViewModel: SharedUiViewModel by activityViewModels()

    private var _binding: FragmentPackagingBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: PackagingContentAdapter

    private var currentUser: UserData? = null
    private var currentPackaging: Packaging? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
        observeSharedViewModel()
        setupClickListeners()
    }

    /**
     * Настроить RecyclerView
     */
    private fun setupRecyclerView() {
        adapter = PackagingContentAdapter {
            // TODO: Implement navigation to product details
            showDialog("Навигация к продукту пока не реализована в новой архитектуре")
        }

        binding.rvPackagingProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingProducts.adapter = adapter
    }

    /**
     * Наблюдать за состоянием ViewModel
     */
    private fun observeViewModel() {
        // Состояние упаковки
        viewModel.packagingState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Idle -> {
                    // Ничего не делаем
                }
                is UiState.Loading -> {
                    // Показать загрузку если нужно
                }
                is UiState.Success -> {
                    updatePackagingUI(state.data)
                }
                is UiState.Error -> {
                    showErrorDialog(state.exception)
                }
            }
        }

        // Состояние действий
        viewModel.actionState.collectIn(viewLifecycleOwner) { state ->
            when (state) {
                is PackagingViewModel.ActionState.Idle -> {
                    // Ничего не делаем
                }
                is PackagingViewModel.ActionState.InProgress -> {
                    // Показать индикатор загрузки
                }
                is PackagingViewModel.ActionState.Success -> {
                    showDialog(state.message)
                    viewModel.resetActionState()
                }
                is PackagingViewModel.ActionState.Error -> {
                    showErrorDialog(state.exception)
                    viewModel.resetActionState()
                }
            }
        }
    }

    /**
     * Наблюдать за общими событиями
     */
    private fun observeSharedViewModel() {
        // TODO: Implement user data observation when available
        // sharedUiViewModel.userData.collectIn(viewLifecycleOwner) { user ->
        //     currentUser = user
        //     updateEditButtonVisibility()
        // }

        sharedUiViewModel.errorMessages.collectEventsIn(viewLifecycleOwner) { message ->
            showDialog(message)
        }
    }

    /**
     * Обновить UI на основе Packaging Domain Model
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePackagingUI(packaging: Packaging) {
        currentPackaging = packaging

        with(binding) {
            tvPackagingSerial.text = packaging.serialNumber
            // TODO: Add performedBy information when available in domain model
            tvCreatedBy.text = "Unknown" // packaging.performedBy?.name
            tvCreatedAt.text = formatIsoToUi(packaging.createdDate)

            val products = packaging.products
            val uiItems = products
                .map { p ->
                    PackagingContentUiItem(
                        id = p.id.toInt(),
                        serialNumber = p.serialNumber,
                        processName = "Process ${p.processId}" // TODO: Get actual process name
                    )
                }
                .sortedBy { it.serialNumber }
            adapter.submitList(uiItems)

            tvItemsSummary.text = getString(R.string.items_count, products.size)

            updateEditButtonVisibility()
        }
    }

    /**
     * Настроить обработчики клика
     */
    private fun setupClickListeners() {
        binding.btnEdit.setOnClickListener {
            // TODO: Implement navigation to edit packaging
            showDialog("Редактирование упаковки пока не реализовано в новой архитектуре")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateEditButtonVisibility() {
        val b = _binding ?: return

        // TODO: Implement proper user role checking
        val role = UserRole.WORKER // currentUser?.role
        val canEdit = currentPackaging?.let { packaging ->
            // TODO: Check if packaging belongs to order, creation date, etc.
            role == UserRole.ADMIN || role == UserRole.MASTER
        } ?: false

        b.btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPackagingProducts.adapter = null
        _binding = null
    }
}
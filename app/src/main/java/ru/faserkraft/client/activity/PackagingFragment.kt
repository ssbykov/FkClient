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
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.PackagingContentAdapter
import ru.faserkraft.client.adapter.PackagingContentUiItem
import ru.faserkraft.client.databinding.FragmentPackagingBinding
import ru.faserkraft.client.dto.PackagingDto
import ru.faserkraft.client.model.UserData
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.utils.formatIsoToUi
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PackagingFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentPackagingBinding

    private lateinit var adapter: PackagingContentAdapter

    private var currentUser: UserData? = null
    private var currentPackaging: PackagingDto? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            currentUser = user
            updateEditButtonVisibility()
        }

        // номер упаковки
        viewModel.packagingState.observe(viewLifecycleOwner) { packaging ->
            currentPackaging = packaging

            binding.tvPackagingSerial.text = packaging?.serialNumber
            binding.tvCreatedBy.text = packaging?.performedBy?.name
            binding.tvCreatedAt.text = formatIsoToUi(packaging?.performedAt)

            updateEditButtonVisibility()
        }

        // адаптер без чекбоксов, просто отображение списка
        adapter = PackagingContentAdapter {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.handleProductQr(it)
                findNavController().navigate(
                    R.id.action_packagingFragment_to_productFullFragment
                )
            }

        }

        binding.rvPackagingProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingProducts.adapter = adapter

        // наполнение списка содержимым текущей упаковки
        viewModel.packagingState.observe(viewLifecycleOwner) {
            // UI‑модель под item_packaging_content_product
            val products = it?.products ?: emptyList()
            val uiItems = products.map { p ->
                PackagingContentUiItem(
                    id = p.id,
                    serialNumber = p.serialNumber,
                    processName = p.process.name
                )
            }
            adapter.submitList(uiItems)

            binding.tvItemsSummary.text = getString(
                R.string.items_count,
                products.size
            )
        }

        // ошибки
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

        // кнопка закрытия (или назад)
        binding.btnClose.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // кнопка редактирования
        binding.btnEdit.setOnClickListener {
            val action =
                PackagingFragmentDirections
                    .actionPackagingFragmentToNewPackagingFragment(viewModel.packagingState.value)
            findNavController().navigate(action)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateEditButtonVisibility() {
        val role = currentUser?.role
        val userEmail = currentUser?.email
        val packagingEmail = currentPackaging?.performedBy?.user?.email
        val packagingDate = currentPackaging?.performedAt
        val shipmentAt = currentPackaging?.shipmentAt

        val isNotShipped = shipmentAt.isNullOrBlank()

        val canEdit =
            role == UserRole.ADMIN ||
                    role == UserRole.MASTER ||
                    (packagingEmail == userEmail && Instant.parse(packagingDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate() == LocalDate.now()) &&
                    isNotShipped

        binding.btnEdit.visibility = if (canEdit) View.VISIBLE else View.GONE
    }
}

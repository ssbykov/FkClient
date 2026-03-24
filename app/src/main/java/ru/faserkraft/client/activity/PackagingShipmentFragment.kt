package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.PackagingShipmentAdapter
import ru.faserkraft.client.adapter.PackagingShipmentUiItem
import ru.faserkraft.client.databinding.FragmentPackagingShipmentBinding
import ru.faserkraft.client.dto.PackagingDto
import ru.faserkraft.client.viewmodel.ScannerViewModel
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


class PackagingShipmentFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentPackagingShipmentBinding

    private val args: PackagingShipmentFragmentArgs by navArgs()

    private lateinit var adapter: PackagingShipmentAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPackagingShipmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val process = args.process
        binding.tvProcess.text = process

        adapter = PackagingShipmentAdapter { item, isChecked ->
            val current = adapter.currentList.toMutableList()
            val index = current.indexOfFirst { it.id == item.id }
            if (index != -1) {
                current[index] = current[index].copy(isSelected = isChecked)
                adapter.submitList(current)
            }

            val allSelected = current.isNotEmpty() && current.all { it.isSelected }
            if (binding.cbSelectAll.isChecked != allSelected) {
                binding.cbSelectAll.setOnCheckedChangeListener(null)
                binding.cbSelectAll.isChecked = allSelected
                binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
            }
        }


        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)

        viewModel.packagingBoxes.observe(viewLifecycleOwner) { packaging ->

            val uiItems: List<PackagingShipmentUiItem> = packaging
                .orEmpty()
                .filter { packaging ->
                    packaging.products.any { it.process.name == process }
                }
                .map { p ->
                    PackagingShipmentUiItem(
                        id = p.id,
                        serialNumber = p.serialNumber,
                        itemsCount = p.products.size,
                        isSelected = false,
                    )
                }

            adapter.submitList(uiItems)

            binding.cbSelectAll.setOnCheckedChangeListener(null)
            val allSelected = uiItems.isNotEmpty() && uiItems.all { it.isSelected }
            binding.cbSelectAll.isChecked = allSelected
            binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
        }

        // начальная загрузка (при необходимости передаём process.id)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPackagingInStorage()
        }

        // обработка ошибок
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

        // кнопка "Сохранить" – проставляет shipmentAt
        binding.btnSave.setOnClickListener {
            val selectedUiItems = adapter.currentList.filter { it.isSelected }
            if (selectedUiItems.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Внимание")
                    .setMessage("Вы не выбрали ни одной упаковки для отгрузки")
                    .setPositiveButton("ОК", null)
                    .show()
                return@setOnClickListener
            }

            val now = nowIsoString()

            // у вас в VM есть список PackagingDto -> маппим по id
            val allPackagings = viewModel.packagingBoxes.value.orEmpty()
            val selectedPackagings: List<PackagingDto> =
                selectedUiItems.mapNotNull { ui ->
                    allPackagings.find { it.id == ui.id }?.copy(
                        shipmentAt = now
                    )
                }

            if (selectedPackagings.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Ошибка")
                    .setMessage("Не удалось сопоставить выбранные упаковки с данными")
                    .setPositiveButton("ОК", null)
                    .show()
                return@setOnClickListener
            }

//            viewLifecycleOwner.lifecycleScope.launch {
//                val result = viewModel.setShipmentForPackagings(selectedPackagings)
//                result.onSuccess {
//                    Snackbar.make(
//                        requireView(),
//                        "Отгрузка оформлена для ${selectedPackagings.size} упаковок",
//                        Snackbar.LENGTH_SHORT
//                    ).show()
//
//                    val navOptions = NavOptions.Builder()
//                        .setPopUpTo(R.id.scannerFragment, false)
//                        .build()
//
//                    findNavController().navigate(
//                        R.id.action_newPackagingShipmentFragment_to_packagingFragment,
//                        null,
//                        navOptions
//                    )
//                }.onFailure {
//                    // ошибка покажется через errorState
//                }
//            }
        }
    }

    // "выбрать все" / "снять все"
    private val selectAllListener =
        CompoundButton.OnCheckedChangeListener { _, isChecked ->
            val updated = adapter.currentList.map { it.copy(isSelected = isChecked) }
            adapter.submitList(updated)
        }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowIsoString(): String {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
}

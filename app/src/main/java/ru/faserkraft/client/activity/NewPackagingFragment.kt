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
import ru.faserkraft.client.adapter.PackagingProductUiItem
import ru.faserkraft.client.adapter.PackagingProductsAdapter
import ru.faserkraft.client.databinding.FragmentNewPackagingBinding
import ru.faserkraft.client.dto.PackagingCreateDto
import ru.faserkraft.client.viewmodel.ScannerViewModel

class NewPackagingFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentNewPackagingBinding

    private lateinit var adapter: PackagingProductsAdapter

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentNewPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // пример: номер упаковки приходит из VM или генерится заранее
        viewModel.packagingState.observe(viewLifecycleOwner) { packaging ->
            binding.tvPackagingSerial.text = packaging?.serialNumber
        }

        adapter = PackagingProductsAdapter { item, isChecked ->
            // обновляем список с учётом изменения выбранности
            val current = adapter.currentList.toMutableList()
            val index = current.indexOfFirst { it.id == item.id }
            if (index != -1) {
                current[index] = current[index].copy(isSelected = isChecked)
                adapter.submitList(current)
            }

            // если хотя бы один не выбран – снимаем "выбрать все", иначе ставим
            val allSelected = adapter.currentList.all { it.isSelected }
            if (binding.cbSelectAll.isChecked != allSelected) {
                binding.cbSelectAll.setOnCheckedChangeListener(null)
                binding.cbSelectAll.isChecked = allSelected
                binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
            }
        }

        binding.rvProducts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProducts.adapter = adapter

        // обработчик "выбрать все"
        binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)

        // загрузка списка доступных продуктов для упаковки
        viewModel.availableProductsForPackaging.observe(viewLifecycleOwner) { products ->
            // маппинг к UI‑модели
            val uiItems = products.map { p ->
                PackagingProductUiItem(
                    id = p.id,
                    serialNumber = p.serialNumber,
                    processName = p.process.name,
                    isSelected = false
                )
            }
            adapter.submitList(uiItems)
            // сброс чекбокса "выбрать все"
            binding.cbSelectAll.setOnCheckedChangeListener(null)
            binding.cbSelectAll.isChecked = false
            binding.cbSelectAll.setOnCheckedChangeListener(selectAllListener)
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

        // создание упаковки
        binding.btnCreate.setOnClickListener {
            val serial = binding.tvPackagingSerial.text?.toString().orEmpty()
            val selectedIds = adapter.currentList
                .filter { it.isSelected }
                .map { it.id }

            if (selectedIds.isEmpty()) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Необходимо выбрать хотя бы один продукт!")
                    .setPositiveButton("ОК", null)
                    .show()
                return@setOnClickListener
            }

            val newPackaging = PackagingCreateDto(
                serialNumber = serial,
                products = selectedIds
            )

            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.createPackaging(newPackaging)
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        // начальная загрузка
//        viewModel.loadAvailableProductsForPackaging()
//        viewModel.generateNewPackagingSerialIfNeeded()
    }

    private val selectAllListener =
        { _: android.widget.CompoundButton, isChecked: Boolean ->
            val updated = adapter.currentList.map { it.copy(isSelected = isChecked) }
            adapter.submitList(updated)
        }
}

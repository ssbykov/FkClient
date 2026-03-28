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
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.faserkraft.client.adapter.ModuleTypeDto
import ru.faserkraft.client.adapter.ShippedPartitionsAdapter
import ru.faserkraft.client.adapter.ShippedPartitionsUiItem
import ru.faserkraft.client.databinding.FragmentShippedPartitionsBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ShippedPartitionsFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()
    private lateinit var binding: FragmentShippedPartitionsBinding
    private lateinit var adapter: ShippedPartitionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShippedPartitionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ShippedPartitionsAdapter { item ->
            // При клике на отгруженную партию — переход к деталям (если нужно)
            // Пока без переходов, можно добавить при необходимости
        }

        binding.rvShippedPartitions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShippedPartitions.adapter = adapter

        // empty view observer
        val emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()

        // первоначальная загрузка
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getShippedPackaging()
        }

        // данные
        viewModel.shippedPackaging.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                return@observe
            }

            val uiList = list
                .groupBy { (it.shipmentAt ?: "").take(10) } // Группируем по дате
                .map { (shipmentDate, shipments) ->
                    // Все продукты за эту дату
                    val allProducts = shipments.flatMap { it.products }

                    // Общее количество упаковок (отгрузок) за дату
                    val packagingCount = shipments.size

                    // Общее количество модулей за дату
                    val moduleCount = allProducts.size

                    // Детализация по процессам (чипам)
                    val moduleTypes = allProducts
                        .groupBy { it.process.id to it.process.name }
                        .map { (key, products) ->
                            val (processId, processName) = key
                            ModuleTypeDto(
                                type = processName,
                                count = products.size
                            )
                        }
                        .sortedByDescending { it.count }

                    ShippedPartitionsUiItem(
                        shipmentDate = shipmentDate,
                        packagingCount = packagingCount,
                        moduleCount = moduleCount,
                        moduleTypes = moduleTypes
                    )
                }
                .sortedByDescending { it.shipmentDate } // Сортируем по дате (новые сначала)

            adapter.submitList(uiList)
        }

        // состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val isLoading = state.isLoading
            binding.swipeRefreshShipped.isRefreshing = isLoading
            binding.swipeRefreshShipped.isEnabled = !isLoading
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

        // pull-to-refresh
        binding.swipeRefreshShipped.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getShippedPackaging()
            }
        }
    }

    private fun checkEmpty() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmptyShipped.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvShippedPartitions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}

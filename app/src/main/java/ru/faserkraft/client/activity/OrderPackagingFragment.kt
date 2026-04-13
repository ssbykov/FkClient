package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.ModuleTypeUi
import ru.faserkraft.client.adapter.PackagingListAdapter
import ru.faserkraft.client.adapter.PackagingListUiItem
import ru.faserkraft.client.databinding.FragmentOrderPackagingBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class OrderPackagingFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentOrderPackagingBinding? = null
    private val binding get() = _binding!!

    // Навигационные аргументы (safeargs)
    private val args: OrderPackagingFragmentArgs by navArgs()
    private lateinit var adapter: PackagingListAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderPackagingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val orderId = args.orderId

        // Тот же адаптер, что и в ShippedByDateFragment
        adapter = PackagingListAdapter { item ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.handlePackagingSerialQr(item.serialNumber)
                if (_binding == null) return@launch
                findNavController().navigate(R.id.action_orderPackagingFragment_to_packagingFragment)
            }
        }

        binding.rvPackagingStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPackagingStats.adapter = adapter

        // Подписываемся на заказы
        viewModel.orders.observe(viewLifecycleOwner) { ordersList ->
            val order = ordersList?.find { it.id == orderId }

            if (order != null) {
                // Обновляем заголовок
                // Стало:
                binding.tvStatsTitle.text =
                    getString(R.string.packaging_title_format, order.contractNumber)

                // Маппим упаковки заказа в UI модели
                val uiItems = order.packaging.map { box ->
                    // ВАЖНО: убедитесь, что обращаетесь к правильному полю у продукта
                    val groups = box.products.groupBy { it.process.name }

                    val types = groups.map { (name, list) ->
                        ModuleTypeUi(name = name, count = list.size)
                    }

                    PackagingListUiItem(
                        id = box.id,
                        serialNumber = box.serialNumber,
                        totalCount = box.products.size,
                        types = types
                    )
                }

                adapter.submitList(uiItems)

                // Показываем/скрываем пустой стейт
                val isEmpty = uiItems.isEmpty()
                binding.tvEmptyStorage.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.rvPackagingStats.visibility = if (isEmpty) View.GONE else View.VISIBLE
            } else {
                binding.tvEmptyStorage.visibility = View.VISIBLE
                binding.rvPackagingStats.visibility = View.GONE
            }
        }

        // Если список заказов пуст, запрашиваем его заново
        if (viewModel.orders.value.isNullOrEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getOrders()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPackagingStats.adapter = null
        _binding = null
    }
}
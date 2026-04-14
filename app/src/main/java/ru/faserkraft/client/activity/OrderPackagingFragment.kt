package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        var itemTouchHelper: ItemTouchHelper? = null

        viewModel.orders.observe(viewLifecycleOwner) { ordersList ->
            val order = ordersList?.find { it.id == orderId }

            if (order != null) {
                val isClosed = order.shipmentDate != null

                // 1. Если заказ НЕ закрыт и свайп еще не установлен — создаем и крепим его
                if (!isClosed && itemTouchHelper == null) {
                    val swipeCallback =
                        object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
                            override fun onMove(
                                recyclerView: RecyclerView,
                                viewHolder: RecyclerView.ViewHolder,
                                target: RecyclerView.ViewHolder
                            ): Boolean = false

                            override fun onSwiped(
                                viewHolder: RecyclerView.ViewHolder,
                                direction: Int
                            ) {
                                val position = viewHolder.bindingAdapterPosition
                                if (position == RecyclerView.NO_POSITION) return

                                val item = adapter.currentList[position]

                                AlertDialog.Builder(requireContext())
                                    .setTitle("Отвязка упаковки")
                                    .setMessage("Вы уверены, что хотите отвязать упаковку ${item.serialNumber} от этого заказа?")
                                    .setPositiveButton("Да") { dialog, _ ->
                                        dialog.dismiss()
                                        viewLifecycleOwner.lifecycleScope.launch {
                                            val result = viewModel.detachPackagingFromOrder(
                                                orderId,
                                                listOf(item.id)
                                            )
                                            if (!result.isSuccess && _binding != null) {
                                                adapter.notifyItemChanged(position)
                                            }
                                        }
                                    }
                                    .setNegativeButton("Нет") { dialog, _ ->
                                        dialog.dismiss()
                                        adapter.notifyItemChanged(position)
                                    }
                                    .setOnCancelListener {
                                        adapter.notifyItemChanged(position)
                                    }
                                    .show()
                            }
                        }
                    itemTouchHelper = ItemTouchHelper(swipeCallback)
                    itemTouchHelper?.attachToRecyclerView(binding.rvPackagingStats)
                }
                // 2. Если заказ ЗАКРЫТ, но свайп был установлен — полностью отключаем его
                else if (isClosed && itemTouchHelper != null) {
                    itemTouchHelper?.attachToRecyclerView(null)
                    itemTouchHelper = null
                }

                // --- Здесь остается ваш текущий код обновления UI ---
                binding.tvStatsTitle.text =
                    getString(R.string.packaging_title_format, order.contractNumber)

                val uiItems = order.packaging.map { box ->
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

                val isEmpty = uiItems.isEmpty()
                binding.tvEmptyStorage.visibility = if (isEmpty) View.VISIBLE else View.GONE
                binding.rvPackagingStats.visibility = if (isEmpty) View.GONE else View.VISIBLE

            } else {
                binding.tvEmptyStorage.visibility = View.VISIBLE
                binding.rvPackagingStats.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPackagingStats.adapter = null
        _binding = null
    }
}
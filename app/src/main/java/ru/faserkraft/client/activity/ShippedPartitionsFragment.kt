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
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.ModuleTypeDto
import ru.faserkraft.client.adapter.ShippedPartitionsAdapter
import ru.faserkraft.client.adapter.ShippedPartitionsUiItem
import ru.faserkraft.client.databinding.FragmentShippedPartitionsBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ShippedPartitionsFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentShippedPartitionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ShippedPartitionsAdapter
    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentShippedPartitionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ShippedPartitionsAdapter { item ->
            if (_binding == null) return@ShippedPartitionsAdapter

            val bundle = Bundle().apply {
                putString("shipmentDate", item.shipmentDate)
            }
            findNavController().navigate(R.id.action_global_shippedByDateFragment, bundle)
        }

        binding.rvShippedPartitions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvShippedPartitions.adapter = adapter

        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getShippedPackaging()
        }

        viewModel.shippedPackaging.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                return@observe
            }

            val uiList = list
                .groupBy { (it.shipmentAt ?: "").take(10) }
                .map { (shipmentDate, shipments) ->
                    val allProducts = shipments.flatMap { it.products }
                    val packagingCount = shipments.size
                    val moduleCount = allProducts.size

                    // Сначала формируем и сортируем типы модулей
                    val moduleTypes = allProducts
                        .groupBy { it.process.id to it.process.name }
                        .map { (key, products) ->
                            ModuleTypeDto(
                                type = key.second, // processName
                                count = products.size
                            )
                        }
                        .sortedByDescending { it.count }

                    // Затем возвращаем итоговый объект
                    ShippedPartitionsUiItem(
                        shipmentDate = shipmentDate,
                        packagingCount = packagingCount,
                        moduleCount = moduleCount,
                        moduleTypes = moduleTypes
                    )
                }
                .sortedByDescending { it.shipmentDate } // Сортируем итоговый список по дате

            adapter.submitList(uiList)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val b = _binding ?: return@observe
            val isLoading = state.isLoading
            b.swipeRefreshShipped.isRefreshing = isLoading
            b.swipeRefreshShipped.isEnabled = !isLoading
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    if (!isAdded) return@collect

                    activeDialog?.dismiss()
                    activeDialog = AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            viewModel.resetIsHandled()
                            dialog.dismiss()
                            activeDialog = null
                        }
                        .also { builder ->
                            builder.setOnDismissListener { activeDialog = null }
                        }
                        .show()
                }
            }
        }

        binding.swipeRefreshShipped.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getShippedPackaging()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activeDialog?.dismiss()
        activeDialog = null

        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }

        binding.rvShippedPartitions.adapter = null
        _binding = null
    }

    private fun checkEmpty() {
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyShipped.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvShippedPartitions.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
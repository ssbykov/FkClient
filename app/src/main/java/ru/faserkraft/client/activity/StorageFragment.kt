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
import ru.faserkraft.client.adapter.ProductsStorageAdapter
import ru.faserkraft.client.adapter.ProductsStorageUiItem
import ru.faserkraft.client.databinding.FragmentProductsStorageBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class StorageFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentProductsStorageBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ProductsStorageAdapter

    private lateinit var emptyObserver: RecyclerView.AdapterDataObserver

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductsStorageBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProductsStorageAdapter { item ->
            val action = StorageContainerFragmentDirections
                .actionStorageContainerFragmentToPackagingListFragment(item.process)
            findNavController().navigate(action)
        }

        binding.rvProductsStats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProductsStats.adapter = adapter

        // empty view observer — теперь через поле класса
        emptyObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() = checkEmpty()
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = checkEmpty()
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = checkEmpty()
        }
        adapter.registerAdapterDataObserver(emptyObserver)
        emptyObserver.onChanged()

        // первоначальная загрузка
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getPackagingInStorage()
        }

        // данные
        viewModel.packagingBoxes.observe(viewLifecycleOwner) { list ->
            if (list.isNullOrEmpty()) {
                adapter.submitList(emptyList())
                return@observe
            }

            val uiList = list
                .flatMap { it.products }
                .groupBy { it.process.id to it.process.name }
                .map { (key, products) ->
                    val (processId, processName) = key

                    val packagingCount = list.count { packaging ->
                        packaging.products.any { it.process.id == processId }
                    }

                    ProductsStorageUiItem(
                        id = processId,
                        process = processName,
                        productCount = products.size,
                        packagingCount = packagingCount
                    )
                }

            adapter.submitList(uiList)
        }

        // состояние загрузки
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val isLoading = state.isLoading
            binding.swipeRefreshStats.isRefreshing = isLoading
            binding.swipeRefreshStats.isEnabled = !isLoading
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
        binding.swipeRefreshStats.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.getPackagingInStorage()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // отписываемся от observer — предотвращаем утечку через адаптер
        if (::emptyObserver.isInitialized) {
            adapter.unregisterAdapterDataObserver(emptyObserver)
        }
        // обнуляем adapter у RecyclerView — разрываем цикл RV → Adapter → View
        binding.rvProductsStats.adapter = null
        // обнуляем binding — Fragment живёт дольше своего View
        _binding = null
    }

    private fun checkEmpty() {
        // _binding может быть null если вызывается после onDestroyView
        val b = _binding ?: return
        val isEmpty = adapter.itemCount == 0
        b.tvEmptyStorage.visibility = if (isEmpty) View.VISIBLE else View.GONE
        b.rvProductsStats.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }
}
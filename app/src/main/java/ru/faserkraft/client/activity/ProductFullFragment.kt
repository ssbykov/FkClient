package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import ru.faserkraft.client.adapter.StepsAdapter
import ru.faserkraft.client.databinding.FragmentProductFullBinding
import ru.faserkraft.client.utils.formatIsoToUi
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ProductFullFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentProductFullBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductFullBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = StepsAdapter()
        binding.rvSteps.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSteps.adapter = adapter


        viewModel.productState.observe(viewLifecycleOwner) { product ->
            product?: return@observe

            binding.tvProductNumber.text = "Модуль: ${product.serialNumber}"
            binding.tvTechProcess.text = "Техпроцесс: ${product.process.name}"
            binding.tvStartAt.text = "Запуск: ${formatIsoToUi(product.createdAt)}"

            adapter.submitList(product.steps)
        }
    }
}
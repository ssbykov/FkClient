package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.databinding.FragmentProductBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel

class ProductFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentProductBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.productState.observe(viewLifecycleOwner) { product ->
            product ?: return@observe
            with(binding) {
                tvProcess.text = product.process.name
                tvSerial.text = product.serialNumber
                tvCreated.text = product.createdAt
            }
        }

        binding.btnDone.setOnClickListener {
            viewModel.resetIsHandled()
            findNavController().navigateUp()
        }

    }

}
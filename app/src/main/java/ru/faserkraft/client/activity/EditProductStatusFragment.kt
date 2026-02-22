package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import ru.faserkraft.client.databinding.FragmentEditStatusProductBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel


class EditProductStatusFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentEditStatusProductBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditStatusProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        viewModel.productState.observe(viewLifecycleOwner) { state ->
            binding.tvSerial.text = state?.serialNumber
            binding.tvProcess.text = state?.process?.name
            binding.tvCurrentStatus.text = state?.status?.label
        }

        // обработка ошибок как у тебя
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

        binding.btnChangeStatus.setOnClickListener {
            val product = viewModel.productState.value
            if (product == null) {
                AlertDialog.Builder(requireContext())
                    .setMessage("Продукт не загружен")
                    .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            val checkedId = binding.rgStatus.checkedRadioButtonId
            if (checkedId == -1) {
                findNavController().navigateUp()
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val result = when (checkedId) {
                    binding.rbNormal.id -> viewModel.setProductStatusNormal(product.id)
                    binding.rbRestore.id -> viewModel.setProductStatusRework(product.id)
                    binding.rbScrap.id  -> viewModel.setProductStatusScrap(product.id)
                    else -> null
                }

                result?.onSuccess {
                    findNavController().navigateUp()
                }?.onFailure {
                    // ошибка уже уйдет в errorState
                }
            }
        }
    }

}
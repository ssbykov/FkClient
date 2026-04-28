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
import ru.faserkraft.client.dto.ProductStatusDto
import ru.faserkraft.client.viewmodel.ScannerViewModel


class EditProductStatusFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentEditStatusProductBinding? = null
    private val binding get() = _binding!!

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditStatusProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.productState.observe(viewLifecycleOwner) { state ->
            binding.tvSerial.text = state?.serialNumber
            binding.tvProcess.text = state?.process?.name
            state?.status?.let { status ->
                binding.tvCurrentStatus.text = getString(status.titleRes)
            }
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            val b = _binding ?: return@observe
            b.btnChangeStatus.isEnabled = !state.isActionInProgress
            b.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE
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
                        .also { it.setOnDismissListener { activeDialog = null } }
                        .show()
                }
            }
        }

        binding.btnChangeStatus.setOnClickListener {
            val product = viewModel.productState.value
            if (product == null) {
                activeDialog?.dismiss()
                activeDialog = AlertDialog.Builder(requireContext())
                    .setMessage("Продукт не загружен")
                    .setPositiveButton("ОК") { dialog, _ ->
                        dialog.dismiss()
                        activeDialog = null
                    }
                    .show()
                return@setOnClickListener
            }

            val checkedId = binding.rgStatus.checkedRadioButtonId

            if (checkedId == -1) {
                findNavController().navigateUp()
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val result = when (checkedId) {
                    binding.rbNormal.id -> viewModel.setProductStatus(
                        product.id, ProductStatusDto.NORMAL
                    )
                    binding.rbRestore.id -> viewModel.setProductStatus(
                        product.id, ProductStatusDto.REWORK
                    )
                    binding.rbScrap.id -> viewModel.setProductStatus(
                        product.id, ProductStatusDto.SCRAP
                    )
                    else -> null
                }

                // проверяем что View ещё жива перед навигацией
                if (_binding == null) return@launch

                result?.onSuccess {
                    findNavController().navigateUp()
                }
                // onFailure — ошибка уйдёт в errorState
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }
}
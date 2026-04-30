package ru.faserkraft.client.presentation.product

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import ru.faserkraft.client.databinding.FragmentEditStatusProductBinding
import ru.faserkraft.client.domain.model.ProductStatus
import ru.faserkraft.client.presentation.ui.collectFlow
import ru.faserkraft.client.data.mapper.toDisplayString

class EditProductStatusFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentEditStatusProductBinding? = null
    private val binding get() = _binding!!

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentEditStatusProductBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeState()
        observeEvents()
        setupSaveButton()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.btnChangeStatus.isEnabled = !state.isActionInProgress
            b.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE

            val product = state.product ?: return@collectFlow
            b.tvSerial.text = product.serialNumber
            b.tvProcess.text = product.process.name
            b.tvCurrentStatus.text = product.status.toDisplayString()
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowError -> showDialog(event.message)
                else -> Unit
            }
        }
    }

    // ---------- Save ----------

    private fun setupSaveButton() {
        binding.btnChangeStatus.setOnClickListener {
            val product = viewModel.uiState.value.product ?: run {
                showDialog("Продукт не загружен")
                return@setOnClickListener
            }

            val checkedId = binding.rgStatus.checkedRadioButtonId

            // Ничего не выбрано — просто уходим
            if (checkedId == -1) {
                findNavController().navigateUp()
                return@setOnClickListener
            }

            val newStatus = when (checkedId) {
                binding.rbNormal.id -> ProductStatus.NORMAL
                binding.rbRestore.id -> ProductStatus.REWORK
                binding.rbScrap.id -> ProductStatus.SCRAP
                else -> null
            } ?: return@setOnClickListener

            // Статус не изменился — просто уходим
            if (newStatus == product.status) {
                findNavController().navigateUp()
                return@setOnClickListener
            }

            viewModel.changeStatus(product.id, newStatus)
            navigateUpOnComplete()
        }
    }

    private fun navigateUpOnComplete() {
        collectFlow(viewModel.uiState) { state ->
            if (!state.isActionInProgress && _binding != null) {
                findNavController().navigateUp()
            }
        }
    }

    // ---------- Dialog ----------

    private fun showDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { d, _ -> d.dismiss(); activeDialog = null }
            .also { it.setOnDismissListener { activeDialog = null } }
            .show()
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
        super.onDestroyView()
    }
}

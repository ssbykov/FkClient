package ru.faserkraft.client.presentation.product

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
import ru.faserkraft.client.utils.ext.showErrorSnackbar

class EditProductStatusFragment : Fragment() {

    private val viewModel: ProductViewModel by activityViewModels()

    private var _binding: FragmentEditStatusProductBinding? = null
    private val binding get() = _binding!!

    // Флаг для безопасного возврата после успешного сохранения
    private var isWaitingForResult = false

    // ---------- Lifecycle ----------

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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // ---------- Observe ----------

    private fun observeState() {
        collectFlow(viewModel.uiState) { state ->
            val b = _binding ?: return@collectFlow

            b.btnChangeStatus.isEnabled = !state.isActionInProgress
            b.progressEdit.visibility =
                if (state.isActionInProgress) View.VISIBLE else View.GONE

            // Навигация при успешном сохранении (если ждали результата и загрузка кончилась)
            if (isWaitingForResult && !state.isActionInProgress) {
                isWaitingForResult = false
                findNavController().navigateUp()
            }

            val product = state.product ?: return@collectFlow
            b.tvSerial.text = product.serialNumber
            b.tvProcess.text = product.process.name
            b.tvCurrentStatus.text = getString(product.status.toUiProductStatus().titleRes)
        }
    }

    private fun observeEvents() {
        collectFlow(viewModel.events) { event ->
            when (event) {
                is ProductEvent.ShowError -> {
                    // Сбрасываем флаг при ошибке, чтобы фрагмент не закрылся
                    isWaitingForResult = false
                    showErrorSnackbar(event.message)
                }

                else -> Unit
            }
        }
    }

    // ---------- Save ----------

    private fun setupSaveButton() {
        binding.btnChangeStatus.setOnClickListener {
            val product = viewModel.uiState.value.product ?: run {
                showErrorSnackbar("Продукт не загружен")
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

            isWaitingForResult = true
            viewModel.changeStatus(product.id, newStatus)
        }
    }
}
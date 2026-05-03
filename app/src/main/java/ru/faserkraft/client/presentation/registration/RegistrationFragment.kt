package ru.faserkraft.client.presentation.registration

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentRegistrationBinding
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.presentation.AppViewModel
import ru.faserkraft.client.presentation.scanner.ScannerViewModel

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private val appViewModel: AppViewModel by activityViewModels()
    private val scannerViewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentRegistrationBinding? = null
    private val binding get() = _binding!!

    private var activeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeUser()
        setupListeners()
    }

    private fun observeUser() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                appViewModel.userData.collect { user ->
                    val b = _binding ?: return@collect
                    user ?: return@collect

                    b.tvEmployeeName.text = user.name
                    b.tvEmail.text = user.email
                    b.tvRole.text = user.role?.value ?: ""
                    b.fabShowQr.isVisible =
                        user.role == UserRole.ADMIN || user.role == UserRole.MASTER
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnDone.setOnClickListener { showResetConfirmationDialog() }

        binding.fabShowQr.setOnClickListener {
            findNavController().navigate(
                R.id.action_registrationFragment_to_qrGenerationFragment
            )
        }
    }

    private fun showResetConfirmationDialog() {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setTitle("Завершить регистрацию?")
            .setMessage("Данные будут очищены, продолжить?")
            .setPositiveButton("Да") { dialog, _ ->
                appViewModel.resetRegistrationData()
                scannerViewModel.clearState()
                dialog.dismiss()
                findNavController().navigateUp()
            }
            .setNegativeButton("Отмена") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { activeDialog = null }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null
        _binding = null
    }
}
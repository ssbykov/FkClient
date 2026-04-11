package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentRegistrationBinding
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.viewmodel.ScannerViewModel

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

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

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            val b = _binding ?: return@observe
            user ?: return@observe

            with(b) {
                tvEmployeeName.text = user.name
                tvEmail.text = user.email
                tvRole.text = user.role?.value ?: ""

                fabShowQr.visibility =
                    if (user.role == UserRole.ADMIN || user.role == UserRole.MASTER) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
            }
        }

        binding.btnDone.setOnClickListener {
            activeDialog?.dismiss()
            activeDialog = AlertDialog.Builder(requireContext())
                .setTitle("Завершить регистрацию?")
                .setMessage("Данные будут очищены, продолжить?")
                .setPositiveButton("Да") { dialog, _ ->
                    viewModel.resetRegistrationData()
                    viewModel.clearUiData()
                    viewModel.resetIsHandled()
                    dialog.dismiss()
                    activeDialog = null
                    findNavController().navigateUp()
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                    activeDialog = null
                }
                .also { it.setOnDismissListener { activeDialog = null } }
                .show()
        }

        binding.fabShowQr.setOnClickListener {
            findNavController().navigate(
                R.id.action_registrationFragment_to_qrGenerationFragment
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeDialog?.dismiss()
        activeDialog = null

        _binding = null
    }
}
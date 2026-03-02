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
import ru.faserkraft.client.databinding.FragmentRegistrationBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel
import ru.faserkraft.client.R
import ru.faserkraft.client.model.UserRole

@AndroidEntryPoint
class RegistrationFragment : Fragment() {

    private val viewModel: ScannerViewModel by activityViewModels()

    private lateinit var binding: FragmentRegistrationBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegistrationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.userData.observe(viewLifecycleOwner) { state ->
            state ?: return@observe
            with(binding) {
                tvEmployeeName.text = state.name
                tvEmail.text = state.email
                tvRole.text = state.role?.value.toString()
            }
        }

        binding.btnDone.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Завершить регистрацию?")
                .setMessage("Данные будут очищены, продолжить?")
                .setPositiveButton("Да") { _, _ ->
                    viewModel.resetRegistrationData()
                    viewModel.resetIsHandled()
                    findNavController().navigateUp()
                }
                .setNegativeButton("Отмена") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            val userRole = user?.role ?: return@observe
            if (userRole == UserRole.ADMIN || userRole == UserRole.MASTER) {
                binding.fabShowQr.visibility = View.VISIBLE
            } else {
                binding.fabShowQr.visibility = View.GONE
            }

        }

        binding.fabShowQr.setOnClickListener {
            findNavController().navigate(
                R.id.action_registrationFragment_to_qrGenerationFragment
            )
        }

    }

}
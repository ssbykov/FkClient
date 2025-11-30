package ru.faserkraft.client.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentRegistrationBinding
import ru.faserkraft.client.databinding.FragmentScannerBinding
import ru.faserkraft.client.viewmodel.ScannerViewModel
import kotlin.getValue

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

        viewModel.registrationState.observe(viewLifecycleOwner) { state ->
                binding.tvEmployeeName.text = state.employeeName
                binding.tvEmail.text = state.email
            }

        binding.btnDone.setOnClickListener {
            viewModel.resetIsHandled()
            findNavController().navigateUp()
        }

    }

}
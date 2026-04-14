package ru.faserkraft.client.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.StoragePageAdapter
import ru.faserkraft.client.databinding.FragmentStorageContainerBinding
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.viewmodel.ScannerViewModel

class StorageContainerFragment : Fragment(R.layout.fragment_storage_container) {

    private val viewModel: ScannerViewModel by activityViewModels()

    private var _binding: FragmentStorageContainerBinding? = null
    private val binding get() = _binding!!

    private var tabLayoutMediator: TabLayoutMediator? = null
    private var activeDialog: AlertDialog? = null // Добавлено для диалога

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentStorageContainerBinding.bind(view)

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            val hasAccess = user?.role == UserRole.MASTER

            binding.noAccessContainer.isVisible = !hasAccess
            binding.viewPagerStorage.isVisible = hasAccess
            binding.tabLayoutStorage.isVisible = hasAccess

            if (hasAccess && binding.viewPagerStorage.adapter == null) {
                setupViewPager()
            }
        }

        // Централизованная обработка ошибок для всех вкладок ViewPager
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.errorState.collect { msg ->
                    // Проверка на isAdded и пустую строку обязательна
                    if (!isAdded || msg.isBlank()) return@collect

                    activeDialog?.dismiss()
                    activeDialog = AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton("ОК") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .also { builder ->
                            builder.setOnDismissListener {
                                viewModel.resetIsHandled()
                                activeDialog = null
                            }
                        }
                        .show()
                }
            }
        }
    }

    private fun setupViewPager() {
        val pagerAdapter = StoragePageAdapter(this)
        binding.viewPagerStorage.adapter = pagerAdapter

        tabLayoutMediator = TabLayoutMediator(binding.tabLayoutStorage, binding.viewPagerStorage) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.storage)
                1 -> getString(R.string.shipped)
                else -> null
            }
        }
        tabLayoutMediator?.attach()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        activeDialog?.dismiss()
        activeDialog = null

        tabLayoutMediator?.detach()
        tabLayoutMediator = null

        binding.viewPagerStorage.adapter = null

        _binding = null
    }
}
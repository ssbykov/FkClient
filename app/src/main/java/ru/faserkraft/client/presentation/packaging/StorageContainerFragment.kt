package ru.faserkraft.client.presentation.packaging

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentStorageContainerBinding
import ru.faserkraft.client.domain.model.UserRole
import ru.faserkraft.client.presentation.AppViewModel
import ru.faserkraft.client.presentation.ui.collectFlow

class StorageContainerFragment : Fragment(R.layout.fragment_storage_container) {

    private val appViewModel: AppViewModel by activityViewModels()

    private var _binding: FragmentStorageContainerBinding? = null
    private val binding get() = _binding!!

    private var tabLayoutMediator: TabLayoutMediator? = null
    private var activeDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStorageContainerBinding.bind(view)

        observeUser()
        observeErrors()
    }

    private fun observeUser() {
        collectFlow(appViewModel.userData) { user ->
            val hasAccess = user?.role == UserRole.MASTER
            val b = _binding ?: return@collectFlow

            b.noAccessContainer.isVisible = !hasAccess
            b.viewPagerStorage.isVisible = hasAccess
            b.tabLayoutStorage.isVisible = hasAccess

            if (hasAccess && b.viewPagerStorage.adapter == null) {
                setupViewPager()
            }
        }
    }

    private fun observeErrors() {
        collectFlow(appViewModel.errorState) { msg ->
            if (!isAdded || msg.isBlank()) return@collectFlow
            showErrorDialog(msg)
        }
    }

    private fun showErrorDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { activeDialog = null }
            .show()
    }

    private fun setupViewPager() {
        binding.viewPagerStorage.adapter = StoragePageAdapter(this)
        tabLayoutMediator = TabLayoutMediator(
            binding.tabLayoutStorage,
            binding.viewPagerStorage
        ) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.orders)
                1 -> getString(R.string.storage)
                else -> null
            }
        }.also { it.attach() }
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
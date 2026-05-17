package ru.faserkraft.client.presentation.product.inventory

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentProductContainerBinding
import ru.faserkraft.client.presentation.app.AppViewModel
import ru.faserkraft.client.presentation.ui.collectFlow

class InventoryContainerFragment : Fragment(R.layout.fragment_product_container) {

    private val appViewModel: AppViewModel by activityViewModels()

    private var _binding: FragmentProductContainerBinding? = null
    private val binding get() = _binding!!

    private var tabLayoutMediator: TabLayoutMediator? = null
    private var activeDialog: AlertDialog? = null

    // ---------- Lifecycle ----------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductContainerBinding.bind(view)

        setupViewPager()
        observeErrors()
    }

    override fun onDestroyView() {
        activeDialog?.dismiss()
        activeDialog = null
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        binding.viewPagerProduct.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- Observers ----------

    private fun observeErrors() {
        collectFlow(appViewModel.errorState) { msg ->
            if (!isAdded || msg.isBlank()) return@collectFlow
            showErrorDialog(msg)
        }
    }

    // ---------- UI Setup ----------

    private fun setupViewPager() {
        binding.viewPagerProduct.adapter = InventoryContainerPageAdapter(this)
        tabLayoutMediator = TabLayoutMediator(
            binding.tabLayoutProduct,
            binding.viewPagerProduct
        ) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.inventory)
                1 -> getString(R.string.scrap)
                else -> null
            }
        }.also { it.attach() }
    }

    // ---------- Dialogs ----------

    private fun showErrorDialog(message: String) {
        activeDialog?.dismiss()
        activeDialog = AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("ОК") { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener { activeDialog = null }
            .show()
    }
}
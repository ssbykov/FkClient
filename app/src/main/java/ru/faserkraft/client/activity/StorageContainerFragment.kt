package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.tabs.TabLayoutMediator
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

        tabLayoutMediator?.detach()
        tabLayoutMediator = null

        binding.viewPagerStorage.adapter = null

        _binding = null
    }
}
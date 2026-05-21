package ru.faserkraft.client.presentation.plan

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayoutMediator
import ru.faserkraft.client.R
import ru.faserkraft.client.databinding.FragmentWorkbenchContainerBinding

class WorkbenchContainerFragment : Fragment(R.layout.fragment_workbench_container) {

    private var _binding: FragmentWorkbenchContainerBinding? = null
    private val binding get() = _binding!!

    private var tabLayoutMediator: TabLayoutMediator? = null

    // ---------- Lifecycle ----------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWorkbenchContainerBinding.bind(view)

        setupViewPager()
    }

    override fun onDestroyView() {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        binding.viewPager.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // ---------- UI Setup ----------

    private fun setupViewPager() {
        binding.viewPager.adapter = WorkbenchPagerAdapter(this)

        tabLayoutMediator = TabLayoutMediator(
            binding.tabLayout,
            binding.viewPager
        ) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.plan)
                1 -> getString(R.string.statistics)
                else -> null
            }
        }.also { it.attach() }
    }
}
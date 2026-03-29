package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.StoragePageAdapter
import ru.faserkraft.client.model.UserRole
import ru.faserkraft.client.viewmodel.ScannerViewModel

class StorageContainerFragment : Fragment(R.layout.fragment_storage_container) {

    private val viewModel: ScannerViewModel by activityViewModels()
    private var currentUserRole: UserRole? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerStorage)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutStorage)
        val noAccessContainer = view.findViewById<View>(R.id.noAccessContainer)

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            currentUserRole = user?.role
        }

        val hasAccess = checkStorageAccess()

        if (hasAccess) {
            noAccessContainer.isVisible = false
            viewPager.isVisible = true
            tabLayout.isVisible = true

            val pagerAdapter = StoragePageAdapter(this)
            viewPager.adapter = pagerAdapter

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.storage)
                    1 -> getString(R.string.shipped)
                    else -> null
                }
            }.attach()
        } else {
            noAccessContainer.isVisible = true
            viewPager.isVisible = false
            tabLayout.isVisible = false
        }
    }

    private fun checkStorageAccess(): Boolean {
        return viewModel.userData.value?.role == UserRole.MASTER
    }
}
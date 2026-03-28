package ru.faserkraft.client.activity

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.faserkraft.client.R
import ru.faserkraft.client.adapter.StoragePageAdapter

class StorageContainerFragment : Fragment(R.layout.fragment_storage_container) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerStorage)
        val tabLayout = view.findViewById<TabLayout>(R.id.tabLayoutStorage)

        val pagerAdapter = StoragePageAdapter(this)
        viewPager.adapter = pagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.storage)
                1 -> getString(R.string.shipped)
                else -> null
            }
        }.attach()
    }
}
package ru.faserkraft.client.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.faserkraft.client.activity.ShippedPartitionsFragment
import ru.faserkraft.client.activity.StorageFragment

class StoragePageAdapter(
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StorageFragment()           // Экран склада
            1 -> ShippedPartitionsFragment() // Экран отгруженных партий
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}

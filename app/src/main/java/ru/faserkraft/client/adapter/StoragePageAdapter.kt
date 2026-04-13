package ru.faserkraft.client.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.faserkraft.client.activity.OrdersFragment
import ru.faserkraft.client.activity.StorageFragment

class StoragePageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> StorageFragment()
            1 -> OrdersFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}

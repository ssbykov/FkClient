package ru.faserkraft.client.presentation.packaging

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.faserkraft.client.presentation.order.OrdersFragment

class StoragePageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OrdersFragment()
            1 -> StorageFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
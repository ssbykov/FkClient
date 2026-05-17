package ru.faserkraft.client.presentation.product.inventory

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter


class InventoryContainerPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProductsInventoryFragment()
            1 -> ProductsReworkScrapFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
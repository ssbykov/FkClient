package ru.faserkraft.client.presentation.inventory

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.faserkraft.client.presentation.inventory.overview.ProductsOverviewFragment
import ru.faserkraft.client.presentation.inventory.overview.ProductsReworkScrapFragment


class InventoryContainerPageAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> ProductsOverviewFragment()
            1 -> ProductsReworkScrapFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
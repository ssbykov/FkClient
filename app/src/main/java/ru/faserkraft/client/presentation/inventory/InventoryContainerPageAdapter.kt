package ru.faserkraft.client.presentation.inventory

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import ru.faserkraft.client.R
import ru.faserkraft.client.presentation.inventory.overview.ProductsOverviewFragment
import ru.faserkraft.client.presentation.inventory.overview.ProductsReworkScrapFragment


enum class InventoryTab(
    val titleRes: Int,
) {
    OVERVIEW(R.string.overview),
    SCRAP(R.string.scrap),
    INVENTORY(R.string.inventory),
}

class InventoryContainerPageAdapter(
    fragment: Fragment,
    private val tabs: List<InventoryTab>,
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = tabs.size

    override fun createFragment(position: Int): Fragment {
        return when (tabs[position]) {
            InventoryTab.OVERVIEW -> ProductsOverviewFragment()
            InventoryTab.SCRAP -> ProductsReworkScrapFragment()
            InventoryTab.INVENTORY -> InventoryListFragment()
        }
    }
}
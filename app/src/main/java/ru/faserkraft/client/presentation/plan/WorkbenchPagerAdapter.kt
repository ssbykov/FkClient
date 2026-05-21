package ru.faserkraft.client.presentation.plan


import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter


class WorkbenchPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DayPlanFragment()
            1 -> StatisticsFragment()
            else -> throw IllegalStateException("Invalid position: $position")
        }
    }
}
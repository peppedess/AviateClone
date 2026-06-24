package com.aviateclone.launcher.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LauncherPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    // 0=SmartStream  1=Home  2=Collections  3=AppList
    override fun getItemCount() = 4
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SmartStreamFragment()
        1 -> HomeFragment()
        2 -> CollectionsFragment()
        3 -> AppListFragment()
        else -> HomeFragment()
    }
}

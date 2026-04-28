package com.aviateclone.launcher.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class LauncherPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    // Aviate: SmartStream(0) | Home(1) | Collections(2)
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> SmartStreamFragment()
        1 -> HomeFragment()
        2 -> CollectionsFragment()
        else -> HomeFragment()
    }
}

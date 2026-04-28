package com.aviateclone.launcher.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aviateclone.launcher.data.AppCategory
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ItemCollectionCardBinding

class CategoryAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var allCategories: List<Pair<AppCategory, List<AppInfo>>> = emptyList()
    private var filtered: List<Pair<AppCategory, List<AppInfo>>> = emptyList()

    private val sharedPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 24) }

    fun submitCategories(map: Map<AppCategory, List<AppInfo>>) {
        allCategories = map.entries
            .filter { it.value.isNotEmpty() }
            .map { it.key to it.value }
            .sortedByDescending { it.second.size }
        filtered = allCategories
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        filtered = if (query.isBlank()) allCategories
        else allCategories.map { (cat, apps) ->
            cat to apps.filter { it.appName.contains(query, true) }
        }.filter { it.second.isNotEmpty() }
        notifyDataSetChanged()
    }

    override fun getItemCount() = filtered.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCollectionCardBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(h: ViewHolder, pos: Int) = h.bind(filtered[pos])

    inner class ViewHolder(private val b: ItemCollectionCardBinding) : RecyclerView.ViewHolder(b.root) {

        private val gridAdapter = AppGridAdapter(onAppClick)

        init {
            b.rvCategoryApps.apply {
                adapter = gridAdapter
                layoutManager = GridLayoutManager(context, 4).apply { initialPrefetchItemCount = 8 }
                setRecycledViewPool(sharedPool)
                isNestedScrollingEnabled = false
            }
        }

        fun bind(pair: Pair<AppCategory, List<AppInfo>>) {
            val (cat, apps) = pair
            b.tvCategoryEmoji.text = cat.emoji
            b.tvCategoryName.text = cat.displayName.uppercase()
            b.tvAppCount.text = "${apps.size}"
            gridAdapter.submitList(apps.take(8))
        }
    }
}

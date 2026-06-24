package com.aviateclone.launcher.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aviateclone.launcher.data.AppCategory
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ItemCategoryCardBinding

class CategoryAdapter(
    private val onAppClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    private var all: List<Pair<AppCategory, List<AppInfo>>> = emptyList()
    private var shown: List<Pair<AppCategory, List<AppInfo>>> = emptyList()
    private val sharedPool = RecyclerView.RecycledViewPool().apply { setMaxRecycledViews(0, 24) }

    private val catColors = mapOf(
        AppCategory.COMMUNICATION to Color.parseColor("#5E6AD2"),
        AppCategory.NAVIGATION    to Color.parseColor("#00695C"),
        AppCategory.MEDIA         to Color.parseColor("#7B2FBE"),
        AppCategory.PRODUCTIVITY  to Color.parseColor("#1565C0"),
        AppCategory.NEWS          to Color.parseColor("#C84B00"),
        AppCategory.ENTERTAINMENT to Color.parseColor("#C2185B"),
        AppCategory.FOOD          to Color.parseColor("#F57F17"),
        AppCategory.FINANCE       to Color.parseColor("#1B7B45"),
        AppCategory.PHOTO         to Color.parseColor("#AD1457"),
        AppCategory.HEALTH        to Color.parseColor("#00838F"),
        AppCategory.OTHER         to Color.parseColor("#455A64")
    )

    fun submitCategories(map: Map<AppCategory, List<AppInfo>>) {
        all = map.entries.filter { it.value.isNotEmpty() }
            .map { it.key to it.value }.sortedByDescending { it.second.size }
        applyDiff(all)
    }

    fun filter(query: String) {
        val filtered = if (query.isBlank()) all
        else all.map { (cat, apps) ->
            cat to apps.filter { it.appName.contains(query, true) }
        }.filter { it.second.isNotEmpty() }
        applyDiff(filtered)
    }

    // FIX: DiffUtil invece di notifyDataSetChanged
    private fun applyDiff(new: List<Pair<AppCategory, List<AppInfo>>>) {
        val old = shown
        val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = old.size
            override fun getNewListSize() = new.size
            override fun areItemsTheSame(o: Int, n: Int) = old[o].first == new[n].first
            override fun areContentsTheSame(o: Int, n: Int) =
                old[o].second.map { it.packageName } == new[n].second.map { it.packageName }
        })
        shown = new
        result.dispatchUpdatesTo(this)
    }

    override fun getItemCount() = shown.size
    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemCategoryCardBinding.inflate(LayoutInflater.from(p.context), p, false))
    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(shown[pos])

    inner class VH(private val b: ItemCategoryCardBinding) : RecyclerView.ViewHolder(b.root) {
        private val gridAdapter = AppGridAdapter(
            onAppClick = onAppClick,
            textColorRes = Color.parseColor("#1C1B1F") // testo scuro su card bianca
        )
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
            b.categoryHeaderLayout.setBackgroundColor(catColors[cat] ?: Color.parseColor("#4D5BBF"))
            gridAdapter.submitList(apps.take(8))
        }
    }
}

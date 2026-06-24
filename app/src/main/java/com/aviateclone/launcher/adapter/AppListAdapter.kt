package com.aviateclone.launcher.adapter

import android.view.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ItemAppRowBinding
import com.aviateclone.launcher.databinding.ItemSectionHeaderBinding

class AppListAdapter(
    // FIX 6: callback ora riceve anche la View dell'icona per zoom-from-icon
    private val onAppClick: (AppInfo, View) -> Unit,
    private val onAppLongClick: (AppInfo) -> Boolean = { false }
) : ListAdapter<AppListAdapter.Item, RecyclerView.ViewHolder>(ItemDiff()) {

    sealed class Item {
        data class Header(val letter: String) : Item()
        data class App(val info: AppInfo)     : Item()
    }

    companion object {
        private const val TYPE_HEADER    = 0
        private const val TYPE_APP       = 1
        private const val PAYLOAD_BADGE  = "badge"
    }

    private var allApps: List<AppInfo> = emptyList()
    private var badges:  Map<String, Int> = emptyMap()

    fun setApps(apps: List<AppInfo>) { allApps = apps; buildList(apps) }

    fun filter(query: String) {
        buildList(if (query.isBlank()) allApps
                  else allApps.filter { it.appName.contains(query, true) })
    }

    fun updateBadges(counts: Map<String, Int>) {
        badges = counts
        for (i in 0 until itemCount) {
            val item = getItem(i)
            if (item is Item.App && (counts[item.info.packageName] ?: 0) != 0)
                notifyItemChanged(i, PAYLOAD_BADGE)
        }
    }

    fun positionForLetter(letter: String): Int {
        if (letter == "#") return 0
        for (i in 0 until itemCount) {
            val item = getItem(i)
            if (item is Item.Header && item.letter == letter) return i
        }
        return 0
    }

    private fun buildList(apps: List<AppInfo>) {
        val items = mutableListOf<Item>()
        var lastKey = ""
        apps.sortedBy { it.appName }.forEach { app ->
            val key = if (app.firstLetter.first().isLetter()) app.firstLetter else "#"
            if (key != lastKey) { items.add(Item.Header(key)); lastKey = key }
            items.add(Item.App(app))
        }
        submitList(items)
    }

    override fun getItemViewType(pos: Int) =
        if (getItem(pos) is Item.Header) TYPE_HEADER else TYPE_APP

    override fun onCreateViewHolder(p: ViewGroup, type: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(p.context)
        return if (type == TYPE_HEADER)
            HeaderVH(ItemSectionHeaderBinding.inflate(inf, p, false))
        else
            AppVH(ItemAppRowBinding.inflate(inf, p, false))
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
        when (val item = getItem(pos)) {
            is Item.Header -> (h as HeaderVH).bind(item)
            is Item.App    -> (h as AppVH).bind(item.info)
        }
    }

    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int, payloads: List<Any>) {
        if (payloads.contains(PAYLOAD_BADGE) && h is AppVH)
            h.bindBadge((getItem(pos) as Item.App).info)
        else super.onBindViewHolder(h, pos, payloads)
    }

    inner class HeaderVH(private val b: ItemSectionHeaderBinding)
        : RecyclerView.ViewHolder(b.root) {
        fun bind(item: Item.Header) { b.headerLetter.text = item.letter }
    }

    inner class AppVH(private val b: ItemAppRowBinding)
        : RecyclerView.ViewHolder(b.root) {

        fun bind(app: AppInfo) {
            b.appIcon.setImageDrawable(app.icon)
            b.appName.text = app.appName
            bindBadge(app)
            b.root.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                // FIX 6: passa b.appIcon come sourceView per zoom-from-icon corretto
                onAppClick(app, b.appIcon)
            }
            b.root.setOnLongClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                onAppLongClick(app)
            }
        }

        fun bindBadge(app: AppInfo) {
            val count = badges[app.packageName] ?: 0
            b.tvBadge?.let {
                it.visibility = if (count > 0) View.VISIBLE else View.GONE
                if (count > 0) it.text = if (count > 99) "99+" else count.toString()
            }
        }
    }

    class ItemDiff : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(a: Item, b: Item) = when {
            a is Item.Header && b is Item.Header -> a.letter == b.letter
            a is Item.App    && b is Item.App    -> a.info.packageName == b.info.packageName
            else                                  -> false
        }
        override fun areContentsTheSame(a: Item, b: Item) = a == b
    }
}

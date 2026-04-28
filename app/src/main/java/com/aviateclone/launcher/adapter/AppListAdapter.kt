package com.aviateclone.launcher.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ItemAppRowBinding
import com.aviateclone.launcher.databinding.ItemSectionHeaderBinding

class AppListAdapter(private val onAppClick: (AppInfo) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object { private const val TYPE_HEADER = 0; private const val TYPE_APP = 1 }

    private sealed class Item {
        data class Header(val letter: String) : Item()
        data class App(val info: AppInfo) : Item()
    }

    private var items: List<Item> = emptyList()
    private var allApps: List<AppInfo> = emptyList()

    fun submitApps(apps: List<AppInfo>) { allApps = apps; buildItems(apps) }

    fun filter(query: String) = buildItems(if (query.isBlank()) allApps else allApps.filter { it.appName.contains(query, true) })

    fun clearFilter() = buildItems(allApps)

    private fun buildItems(apps: List<AppInfo>) {
        val new = mutableListOf<Item>()
        var cur = ""
        apps.forEach { app ->
            if (app.firstLetter != cur) { cur = app.firstLetter; new.add(Item.Header(cur)) }
            new.add(Item.App(app))
        }
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = new.size
            override fun areItemsTheSame(o: Int, n: Int): Boolean {
                val a = items[o]; val b = new[n]
                return when {
                    a is Item.Header && b is Item.Header -> a.letter == b.letter
                    a is Item.App && b is Item.App -> a.info.packageName == b.info.packageName
                    else -> false
                }
            }
            // BUGFIX: areContentsTheSame non deve confrontare Drawable (icon) tramite data class equals.
            // Usiamo un confronto esplicito sui campi dati significativi.
            override fun areContentsTheSame(o: Int, n: Int): Boolean {
                val a = items[o]; val b = new[n]
                if (a is Item.Header && b is Item.Header) return a.letter == b.letter
                if (a is Item.App && b is Item.App) {
                    return a.info.packageName == b.info.packageName &&
                           a.info.appName == b.info.appName &&
                           a.info.category == b.info.category &&
                           a.info.launchCount == b.info.launchCount &&
                           a.info.lastUsed == b.info.lastUsed
                }
                return false
            }
        })
        items = new; diff.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int) = if (items[position] is Item.Header) TYPE_HEADER else TYPE_APP

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val li = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) HeaderVH(ItemSectionHeaderBinding.inflate(li, parent, false))
        else AppVH(ItemAppRowBinding.inflate(li, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.Header -> (holder as HeaderVH).bind(item.letter)
            is Item.App -> (holder as AppVH).bind(item.info)
        }
    }

    override fun getItemCount() = items.size

    inner class HeaderVH(private val b: ItemSectionHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(letter: String) { b.headerLetter.text = letter }
    }

    inner class AppVH(private val b: ItemAppRowBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: AppInfo) {
            b.appIcon.setImageDrawable(app.icon)
            b.appName.text = app.appName
            b.root.setOnClickListener { onAppClick(app) }
        }
    }
}

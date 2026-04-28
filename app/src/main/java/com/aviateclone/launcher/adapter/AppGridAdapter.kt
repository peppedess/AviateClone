package com.aviateclone.launcher.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ItemAppGridBinding

class AppGridAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: ((AppInfo) -> Boolean)? = null
) : ListAdapter<AppInfo, AppGridAdapter.ViewHolder>(Diff()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemAppGridBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val b: ItemAppGridBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(app: AppInfo) {
            b.appIcon.setImageDrawable(app.icon)
            b.appName.text = app.appName
            b.root.setOnClickListener { onAppClick(app) }
            onAppLongClick?.let { b.root.setOnLongClickListener { it(app) } }
        }
    }

    class Diff : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
        // BUGFIX: non confrontare il Drawable (icon) perché non implementa equals() correttamente.
        // Confrontiamo solo i campi dati che determinano se la voce è "cambiata" visivamente.
        override fun areContentsTheSame(a: AppInfo, b: AppInfo) =
            a.packageName == b.packageName &&
            a.appName == b.appName &&
            a.category == b.category &&
            a.launchCount == b.launchCount &&
            a.lastUsed == b.lastUsed
    }
}

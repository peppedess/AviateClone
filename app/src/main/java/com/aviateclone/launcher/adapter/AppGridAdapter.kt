package com.aviateclone.launcher.adapter

import android.content.pm.ShortcutManager
import android.os.Build
import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aviateclone.launcher.data.AppInfo
import com.aviateclone.launcher.databinding.ItemAppGridBinding

class AppGridAdapter(
    private val onAppClick: (AppInfo) -> Unit,
    private val onAppLongClick: ((AppInfo) -> Boolean)? = null,
    private val textColorRes: Int? = null  // null = bianco (wallpaper), non-null = scuro (card)
) : ListAdapter<AppInfo, AppGridAdapter.VH>(Diff()) {

    companion object {
        private const val PAYLOAD_BADGE = "badge"
    }

    private var badges: Map<String, Int> = emptyMap()

    // FIX 3: aggiorna solo le celle con badge cambiato via payload
    fun updateBadges(counts: Map<String, Int>) {
        val old = badges
        badges = counts
        for (i in 0 until itemCount) {
            val pkg = getItem(i).packageName
            if ((old[pkg] ?: 0) != (counts[pkg] ?: 0))
                notifyItemChanged(i, PAYLOAD_BADGE)
        }
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int) =
        VH(ItemAppGridBinding.inflate(LayoutInflater.from(p.context), p, false))

    override fun onBindViewHolder(h: VH, pos: Int) = h.bind(getItem(pos))

    // FIX 3: partial bind per payload badge
    override fun onBindViewHolder(h: VH, pos: Int, payloads: List<Any>) {
        if (payloads.contains(PAYLOAD_BADGE)) h.bindBadge(getItem(pos))
        else super.onBindViewHolder(h, pos, payloads)
    }

    inner class VH(private val b: ItemAppGridBinding) : RecyclerView.ViewHolder(b.root) {

        fun bind(app: AppInfo) {
            b.appIcon.setImageDrawable(app.icon)
            b.appName.text = app.appName
            // Testo scuro se in contesto card, bianco su wallpaper
            textColorRes?.let { b.appName.setTextColor(it) }
            bindBadge(app)
            b.root.setOnClickListener {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onAppClick(app)
            }
            b.root.setOnLongClickListener { v ->
                v.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                if (onAppLongClick != null) onAppLongClick.invoke(app)
                else { showShortcuts(v, app); true }
            }
        }

        fun bindBadge(app: AppInfo) {
            val count = badges[app.packageName] ?: 0
            b.tvBadge?.let {
                it.visibility = if (count > 0) View.VISIBLE else View.GONE
                if (count > 0) it.text = if (count > 99) "99+" else count.toString()
            }
        }

        private fun showShortcuts(v: View, app: AppInfo) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return
            try {
                val sm = v.context.getSystemService(ShortcutManager::class.java) ?: return
                val sc = sm.getShortcuts(ShortcutManager.FLAG_MATCH_DYNAMIC)
                    ?.filter { it.`package` == app.packageName } ?: return
                if (sc.isEmpty()) return
                val popup = PopupMenu(v.context, v)
                sc.forEach { popup.menu.add(it.shortLabel ?: it.id) }
                popup.show()
            } catch (_: Exception) {}
        }
    }

    class Diff : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(a: AppInfo, b: AppInfo) = a.packageName == b.packageName
        override fun areContentsTheSame(a: AppInfo, b: AppInfo) =
            a.packageName == b.packageName && a.appName == b.appName && a.launchCount == b.launchCount
    }
}

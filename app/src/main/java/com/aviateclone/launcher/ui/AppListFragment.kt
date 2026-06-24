package com.aviateclone.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aviateclone.launcher.adapter.AppListAdapter
import com.aviateclone.launcher.databinding.FragmentAppListBinding

class AppListFragment : Fragment() {

    private var _b: FragmentAppListBinding? = null
    private val b get() = _b!!
    private val vm: LauncherViewModel by activityViewModels()
    private lateinit var adapter: AppListAdapter
    private lateinit var llm: LinearLayoutManager

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentAppListBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(b.appListStatusBarSpacer) { v, insets ->
            v.layoutParams.height = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.requestLayout(); insets
        }

        adapter = AppListAdapter(
            // FIX 6: iconView è la view esatta dell'icona cliccata
            onAppClick = { app, iconView ->
                b.etSearch.clearFocus()
                (activity as? MainActivity)?.launchAppFromView(app, iconView)
            },
            onAppLongClick = { app ->
                (activity as? MainActivity)?.showAppOptions(app); true
            }
        )

        llm = LinearLayoutManager(requireContext())
        b.rvAllApps.apply {
            this.adapter = this@AppListFragment.adapter
            layoutManager = llm
            setHasFixedSize(false)
        }

        vm.allApps.observe(viewLifecycleOwner) { adapter.setApps(it) }
        vm.badgeCounts.observe(viewLifecycleOwner) { adapter.updateBadges(it) }

        b.sideAlphabet?.onLetterSelected = { letter ->
            val pos = adapter.positionForLetter(letter)
            llm.scrollToPositionWithOffset(pos, 0)
        }

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                adapter.filter(q)
                b.sideAlphabet?.visibility = if (q.isBlank()) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, a: Int) {}
        })
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

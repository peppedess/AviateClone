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
import com.aviateclone.launcher.adapter.CategoryAdapter
import com.aviateclone.launcher.databinding.FragmentCollectionsBinding

class CollectionsFragment : Fragment() {
    private var _b: FragmentCollectionsBinding? = null
    private val b get() = _b!!
    private val vm: LauncherViewModel by activityViewModels()
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCollectionsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(b.collStatusBarSpacer) { v, insets ->
            v.layoutParams.height = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.requestLayout(); insets
        }

        adapter = CategoryAdapter(onAppClick = { app ->
            (activity as? MainActivity)?.launchApp(app)
        })

        b.rvCollections.apply {
            this.adapter = this@CollectionsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }

        vm.appsByCategory.observe(viewLifecycleOwner) { map ->
            adapter.submitCategories(map)
        }

        b.etCollSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { adapter.filter(s?.toString() ?: "") }
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, a: Int) {}
        })
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

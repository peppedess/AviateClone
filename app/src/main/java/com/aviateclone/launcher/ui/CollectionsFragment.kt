package com.aviateclone.launcher.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.aviateclone.launcher.adapter.CategoryAdapter
import com.aviateclone.launcher.databinding.FragmentCollectionsBinding

class CollectionsFragment : Fragment() {

    private var _b: FragmentCollectionsBinding? = null
    private val b get() = _b!!
    private val vm: LauncherViewModel by activityViewModels()
    private lateinit var catAdapter: CategoryAdapter

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentCollectionsBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Padding status bar per il topBar
        val sbH = resources.getIdentifier("status_bar_height", "dimen", "android")
            .let { if (it > 0) resources.getDimensionPixelSize(it) else 60 }
        b.topBar.setPadding(
            b.topBar.paddingLeft, sbH + 8,
            b.topBar.paddingRight, b.topBar.paddingBottom)

        catAdapter = CategoryAdapter { app -> (activity as? MainActivity)?.launchApp(app) }
        b.rvCollections.apply {
            adapter = catAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        vm.appsByCategory.observe(viewLifecycleOwner) { map ->
            catAdapter.submitCategories(map)
        }
        b.etCollSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b2: Int, c: Int) {
                catAdapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}

package com.aviateclone.launcher.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.aviateclone.launcher.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.ITALIAN)

    private val handler = Handler(Looper.getMainLooper())
    private val clockRunnable = object : Runnable {
        override fun run() { updateClock(); handler.postDelayed(this, 10_000) }
    }

    override fun onCreateView(i: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        _b = FragmentHomeBinding.inflate(i, c, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateClock()
        handler.post(clockRunnable)
    }

    private fun updateClock() {
        val now = Date()
        _b?.tvTime?.text = timeFmt.format(now)
        _b?.tvDate?.text = dateFmt.format(now).replaceFirstChar { it.uppercase() }
    }

    override fun onResume() { super.onResume(); updateClock() }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(clockRunnable)
        _b = null
    }
}

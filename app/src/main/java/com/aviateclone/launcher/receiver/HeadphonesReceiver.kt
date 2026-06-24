package com.aviateclone.launcher.receiver

import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class HeadphonesReceiver : BroadcastReceiver() {

    companion object {
        var onHeadphonesChanged: ((connected: Boolean, name: String) -> Unit)? = null
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                val name  = intent.getStringExtra("name") ?: "Cuffie"
                onHeadphonesChanged?.invoke(state == 1, name)
            }
            BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED,
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)
                val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                val name = try { device?.name ?: "Bluetooth" } catch (_: SecurityException) { "Bluetooth" }
                onHeadphonesChanged?.invoke(state == BluetoothProfile.STATE_CONNECTED, name)
            }
        }
    }
}

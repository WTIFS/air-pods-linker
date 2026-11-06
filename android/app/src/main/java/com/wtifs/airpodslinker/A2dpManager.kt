package com.wtifs.airpodslinker

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat

// Constructed only after permission is granted. Calls also check for revocation.
@SuppressLint("MissingPermission")
class A2dpManager(private val context: Context) {
    private val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    private var proxy: BluetoothProfile? = null
    private var target: BluetoothDevice? = null
    private var binding = false
    private var closed = false
    private var requestedConnection: Boolean? = null
    private val handler = Handler(Looper.getMainLooper())
    private val preferences = context.getSharedPreferences("airpodslinker", Context.MODE_PRIVATE)

    var deviceName by mutableStateOf("No AirPods found")
        private set
    var isConnected by mutableStateOf(false)
        private set
    var bluetoothOn by mutableStateOf(false)
        private set
    var isBusy by mutableStateOf(false)
        private set
    var canToggle by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set

    private fun permitted() = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
        PackageManager.PERMISSION_GRANTED

    private val timeout = Runnable {
        refresh()
        if (requestedConnection != null) {
            requestedConnection = null
            isBusy = false
            refresh()
            message = "Bluetooth did not confirm the request. Try Bluetooth Settings."
        }
    }

    private val listener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, service: BluetoothProfile) {
            if (closed) {
                adapter?.closeProfileProxy(profile, service)
                return
            }
            binding = false
            proxy = service
            refresh()
        }

        override fun onServiceDisconnected(profile: Int) {
            proxy = null
            binding = false
            clearConnection()
            if (!closed) message = "Bluetooth service unavailable. Tap Refresh to retry."
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!closed) refresh()
        }
    }

    init {
        // Bluetooth broadcasts can come from a privileged process with a non-system UID.
        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
        }, ContextCompat.RECEIVER_EXPORTED)
        refresh()
    }

    private fun clearConnection() {
        isConnected = false
        canToggle = false
        isBusy = false
        requestedConnection = null
        handler.removeCallbacks(timeout)
    }

    fun refresh() {
        if (closed) return
        if (!permitted()) {
            clearConnection()
            message = "Bluetooth permission is required."
            return
        }
        try {
            val wasOn = bluetoothOn
            val previousTarget = target?.address
            val wasConnected = isConnected
            bluetoothOn = adapter?.isEnabled == true
            if (!bluetoothOn) {
                clearConnection()
                message = if (adapter == null) "This device does not support Bluetooth." else "Turn on Bluetooth in Settings."
                return
            }
            val paired = adapter?.bondedDevices.orEmpty().sortedBy { it.address }
            val saved = preferences.getString("device_address", null)
            target = paired.firstOrNull { it.address.equals(saved, ignoreCase = true) }
                ?: paired.firstOrNull { it.name?.contains("airpods", ignoreCase = true) == true }
            if (previousTarget != target?.address) clearConnection()
            deviceName = target?.name ?: "No AirPods found"
            preferences.edit().putString("device_address", target?.address).apply()
            if (proxy == null && !binding) {
                binding = adapter?.getProfileProxy(context, listener, BluetoothProfile.A2DP) == true
            }
            val state = target?.let { proxy?.getConnectionState(it) } ?: BluetoothProfile.STATE_DISCONNECTED
            isConnected = state == BluetoothProfile.STATE_CONNECTED
            if (!wasOn || previousTarget != target?.address || wasConnected != isConnected) {
                message = null
            }
            if (hasReachedRequestedState(requestedConnection, state)) {
                requestedConnection = null
                handler.removeCallbacks(timeout)
                message = if (isConnected) "Connected" else "Disconnected"
            }
            isBusy = requestedConnection != null || state == BluetoothProfile.STATE_CONNECTING ||
                state == BluetoothProfile.STATE_DISCONNECTING
            canToggle = target != null && proxy != null && !isBusy
            if (target == null) {
                clearConnection()
                message = "Pair your AirPods in Bluetooth Settings first."
            } else if (proxy == null) {
                message = "Waiting for Bluetooth service. Tap Refresh to retry."
            }
        } catch (_: SecurityException) {
            clearConnection()
            message = "Bluetooth access was denied. Check permissions in Settings."
        }
    }

    fun toggleConnection() {
        if (isBusy) return
        refresh()
        if (!canToggle) return
        val device = target ?: return
        val service = proxy ?: return
        val connect = !isConnected
        try {
            // Android has no public API to control A2DP connections. Some devices
            // allow these methods; others block them or require privileged access.
            val accepted = service.javaClass.getMethod(
                if (connect) "connect" else "disconnect", BluetoothDevice::class.java
            ).invoke(service, device) == true
            if (!accepted) {
                message = "Android rejected the request. Use Bluetooth Settings."
                return
            }
            requestedConnection = connect
            isBusy = true
            canToggle = false
            message = if (connect) "Connecting…" else "Disconnecting…"
            handler.postDelayed(timeout, 15_000)
            // Only actual profile state or a broadcast may confirm success.
            refresh()
        } catch (_: ReflectiveOperationException) {
            message = "This Android device restricts direct Bluetooth control. Use Bluetooth Settings."
        } catch (_: SecurityException) {
            message = "Android denied direct Bluetooth control. Use Bluetooth Settings."
        }
    }

    fun close() {
        if (closed) return
        closed = true
        handler.removeCallbacksAndMessages(null)
        context.unregisterReceiver(receiver)
        proxy?.let { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
        proxy = null
    }
}

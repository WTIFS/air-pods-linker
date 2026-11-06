package com.wtifs.airpodslinker

import android.bluetooth.BluetoothProfile

internal fun hasReachedRequestedState(connect: Boolean?, state: Int): Boolean = when (connect) {
    true -> state == BluetoothProfile.STATE_CONNECTED
    false -> state == BluetoothProfile.STATE_DISCONNECTED
    null -> false
}

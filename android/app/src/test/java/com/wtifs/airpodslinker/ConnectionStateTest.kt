package com.wtifs.airpodslinker

import android.bluetooth.BluetoothProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateTest {
    @Test fun disconnectIsNotConfirmedDuringTransition() {
        assertFalse(hasReachedRequestedState(false, BluetoothProfile.STATE_DISCONNECTING))
        assertFalse(hasReachedRequestedState(false, BluetoothProfile.STATE_CONNECTING))
        assertFalse(hasReachedRequestedState(false, BluetoothProfile.STATE_CONNECTED))
        assertTrue(hasReachedRequestedState(false, BluetoothProfile.STATE_DISCONNECTED))
    }

    @Test fun connectRequiresConfirmedConnection() {
        assertFalse(hasReachedRequestedState(true, BluetoothProfile.STATE_CONNECTING))
        assertFalse(hasReachedRequestedState(true, BluetoothProfile.STATE_DISCONNECTED))
        assertTrue(hasReachedRequestedState(true, BluetoothProfile.STATE_CONNECTED))
    }

    @Test fun noRequestOrUnknownStateCannotSucceed() {
        assertFalse(hasReachedRequestedState(null, BluetoothProfile.STATE_CONNECTED))
        assertFalse(hasReachedRequestedState(null, BluetoothProfile.STATE_DISCONNECTED))
        assertFalse(hasReachedRequestedState(false, -1))
    }
}

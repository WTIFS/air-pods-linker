package com.wtifs.airpodslinker

internal data class PairedDevice(
    val address: String,
    val name: String?,
    val connected: Boolean
)

internal fun selectRecentDevice(
    devices: List<PairedDevice>,
    saved: String?,
    recent: List<String>
): String? = devices.filter {
    it.name?.contains("airpods", ignoreCase = true) == true ||
        it.address.equals(saved, ignoreCase = true) ||
        recent.any { address -> address.equals(it.address, ignoreCase = true) }
}.sortedWith(
    compareByDescending<PairedDevice> { it.connected }
        .thenBy { device -> recent.indexOfFirst { it.equals(device.address, ignoreCase = true) }
            .let { if (it < 0) Int.MAX_VALUE else it } }
        .thenByDescending { it.address.equals(saved, ignoreCase = true) }
        .thenBy { it.address }
).firstOrNull()?.address

// Only newly observed connections move forward; refreshing an existing connection
// must not continually overwrite the order. Simultaneous observations are tied.
internal fun recordConnections(recent: List<String>, current: Set<String>, previous: Set<String>): List<String> {
    val added = (current - previous).filter { it !in recent }.sorted() +
        recent.filter { it in current && it !in previous }
    return added + recent.filter { it !in added }
}

import Foundation

/// Prefer connected AirPods, then system recency. Saved selection breaks ties
/// and keeps a renamed device eligible. Address order is only a final fallback.
func selectPairedDevice<Device>(
    from devices: [Device],
    savedAddress: String?,
    address: (Device) -> String?,
    name: (Device) -> String?,
    connected: (Device) -> Bool = { _ in false },
    lastUsed: (Device) -> Date? = { _ in nil }
) -> Device? {
    func isSaved(_ device: Device) -> Bool {
        guard let savedAddress else { return false }
        return address(device)?.caseInsensitiveCompare(savedAddress) == .orderedSame
    }
    return devices.filter {
        address($0) != nil && (isSaved($0) || name($0)?.localizedCaseInsensitiveContains("airpods") == true)
    }.sorted {
        if connected($0) != connected($1) { return connected($0) }
        let left = lastUsed($0) ?? .distantPast
        let right = lastUsed($1) ?? .distantPast
        if left != right { return left > right }
        if isSaved($0) != isSaved($1) { return isSaved($0) }
        return (address($0) ?? "") < (address($1) ?? "")
    }.first
}

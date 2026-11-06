import Foundation

/// Prefer a still-paired saved device, including one the user has renamed.
/// Otherwise select deterministically so enumeration order cannot switch devices.
func selectPairedDevice<Device>(
    from devices: [Device],
    savedAddress: String?,
    address: (Device) -> String?,
    name: (Device) -> String?
) -> Device? {
    let paired = devices.filter { address($0) != nil }
    if let savedAddress, let saved = paired.first(where: {
        address($0)?.caseInsensitiveCompare(savedAddress) == .orderedSame
    }) {
        return saved
    }
    return paired.filter { name($0)?.localizedCaseInsensitiveContains("airpods") == true }
        .sorted { (address($0) ?? "") < (address($1) ?? "") }.first
}

import Foundation
import Testing
@testable import AirPodsLinker

private struct Device: Equatable {
    let address: String?
    let name: String?
    var connected = false
    var lastUsed: Date? = nil
}

private func select(_ devices: [Device], saved: String? = nil) -> Device? {
    selectPairedDevice(from: devices, savedAddress: saved, address: { $0.address }, name: { $0.name },
                       connected: { $0.connected }, lastUsed: { $0.lastUsed })
}

@Test func staleCacheFallsBackToPairedAirPods() {
    let airpods = Device(address: "AA:02", name: "Chris’s AirPods")
    #expect(select([Device(address: "AA:01", name: "Keyboard"), airpods], saved: "deleted") == airpods)
}

@Test func savedDeviceSurvivesRenameAndAddressCaseChanges() {
    let renamed = Device(address: "AA:02", name: "Headphones")
    #expect(select([Device(address: "AA:01", name: "AirPods"), renamed], saved: "aa:02") == renamed)
}

@Test func missingOrUnpairedDeviceDoesNotProduceTarget() {
    #expect(select([], saved: "old-address") == nil)
    #expect(select([Device(address: "AA:01", name: "Keyboard")]) == nil)
    #expect(select([Device(address: nil, name: "AirPods")]) == nil)
}

@Test func selectionIsStableAcrossEnumerationOrder() {
    let first = Device(address: "AA:01", name: "AIRPODS Pro")
    let second = Device(address: "AA:02", name: "AirPods Max")
    #expect(select([second, first]) == first)
    #expect(select([first, second]) == first)
}

@Test func mostRecentDeviceOverridesSavedSelection() {
    let old = Device(address: "AA:01", name: "AirPods", lastUsed: Date(timeIntervalSince1970: 10))
    let recent = Device(address: "AA:02", name: "AirPods Pro", lastUsed: Date(timeIntervalSince1970: 20))
    #expect(select([old, recent], saved: old.address) == recent)
}

@Test func connectedDeviceTakesPriorityOverDisconnectedHistory() {
    let active = Device(address: "AA:02", name: "AirPods", connected: true)
    let recent = Device(address: "AA:01", name: "AirPods Pro", lastUsed: Date())
    #expect(select([recent, active], saved: recent.address) == active)
}

@Test func multipleConnectedDevicesUseRecency() {
    let old = Device(address: "AA:01", name: "AirPods", connected: true, lastUsed: Date(timeIntervalSince1970: 10))
    let recent = Device(address: "AA:02", name: "AirPods Pro", connected: true, lastUsed: Date(timeIntervalSince1970: 20))
    #expect(select([old, recent]) == recent)
}

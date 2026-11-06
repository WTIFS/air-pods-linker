import Testing
@testable import AirPodsLinker

private struct Device: Equatable {
    let address: String?
    let name: String?
}

private func select(_ devices: [Device], saved: String? = nil) -> Device? {
    selectPairedDevice(from: devices, savedAddress: saved, address: { $0.address }, name: { $0.name })
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

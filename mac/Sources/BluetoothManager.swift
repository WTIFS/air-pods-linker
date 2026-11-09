import IOBluetooth
import ServiceManagement
import Combine
import Foundation

@MainActor
final class BluetoothManager: ObservableObject {
    @Published private(set) var isConnected = false
    @Published private(set) var deviceName = "No AirPods found"
    @Published private(set) var hasDevice = false
    @Published private(set) var isBusy = false
    @Published private(set) var launchAtLogin = false
    @Published private(set) var errorMessage: String?

    private var pollingTask: Task<Void, Never>?
    private var deviceAddress: String?

    init() {
        refresh()
        pollingTask = Task { [weak self] in
            while !Task.isCancelled {
                do { try await Task.sleep(for: .seconds(3)) }
                catch { return }
                self?.refresh()
            }
        }
    }

    deinit { pollingTask?.cancel() }

    func refresh() {
        launchAtLogin = SMAppService.mainApp.status == .enabled
        guard !isBusy else { return }
        // An address object can exist even after a device has been unpaired.
        // Only trust addresses that are still in the paired-device list.
        let paired = IOBluetoothDevice.pairedDevices() as? [IOBluetoothDevice] ?? []
        let saved = UserDefaults.standard.string(forKey: "deviceAddress")
        let device = selectPairedDevice(
            from: paired, savedAddress: saved,
            address: { $0.addressString }, name: { $0.name },
            connected: { $0.isConnected() }, lastUsed: { $0.recentAccessDate() }
        )
        deviceAddress = device?.addressString
        hasDevice = deviceAddress != nil
        deviceName = device?.name ?? (hasDevice ? "AirPods" : "No AirPods found")
        isConnected = device?.isConnected() ?? false
        UserDefaults.standard.set(deviceAddress, forKey: "deviceAddress")
    }

    func toggleConnection() {
        guard !isBusy else { return }
        refresh()
        guard let address = deviceAddress else {
            errorMessage = "Pair your AirPods in Bluetooth Settings first."
            return
        }
        isBusy = true
        errorMessage = nil
        // IOBluetooth's synchronous connection call may block for seconds.
        // Construct and use the device on the worker; never send it across actors.
        Task {
            let result = await Task.detached(priority: .userInitiated) { () -> (Int32, Bool) in
                guard let device = IOBluetoothDevice(addressString: address) else {
                    return (kIOReturnNotFound, false)
                }
                let connect = !device.isConnected()
                let code = connect ? device.openConnection() : device.closeConnection()
                return (code, connect)
            }.value
            isBusy = false
            refresh()
            if result.0 != kIOReturnSuccess {
                errorMessage = "Could not \(result.1 ? "connect" : "disconnect") (\(result.0)). Check Bluetooth Settings and try again."
            } else if deviceAddress == address && isConnected != result.1 {
                errorMessage = "Request sent. Waiting for Bluetooth to update; use Refresh to check."
            }
        }
    }

    func toggleLaunchAtLogin(enabled: Bool) {
        errorMessage = nil
        do {
            if enabled {
                try SMAppService.mainApp.register()
            } else {
                try SMAppService.mainApp.unregister()
            }
            if SMAppService.mainApp.status == .requiresApproval {
                errorMessage = "Allow AirPodsLinker in System Settings → General → Login Items."
            }
        } catch {
            errorMessage = "Could not change Launch at Login: \(error.localizedDescription)"
        }
        launchAtLogin = SMAppService.mainApp.status == .enabled
    }
}

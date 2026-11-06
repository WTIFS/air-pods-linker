import AppKit
import Combine

@main
@MainActor
enum AirPodsLinkerApp {
    static func main() {
        let application = NSApplication.shared
        let delegate = AppDelegate()
        application.delegate = delegate
        application.setActivationPolicy(.accessory)
        withExtendedLifetime(delegate) {
            application.run()
        }
    }
}

@MainActor
final class AppDelegate: NSObject, NSApplicationDelegate {
    private var statusItem: NSStatusItem?
    private var manager: BluetoothManager?
    private var observation: AnyCancellable?

    func applicationDidFinishLaunching(_ notification: Notification) {
        let manager = BluetoothManager()
        self.manager = manager
        let item = NSStatusBar.system.statusItem(withLength: NSStatusItem.squareLength)
        statusItem = item
        if let button = item.button {
            button.image = makeStatusIcon()
            button.imagePosition = .imageOnly
            button.target = self
            button.action = #selector(toggleConnection)
            button.sendAction(on: .leftMouseUp)
        }
        // Published values send before the assignment. Update on the next main
        // actor turn so the tooltip and enabled state reflect the new values.
        observation = manager.objectWillChange.sink { [weak self] _ in
            Task { @MainActor [weak self] in self?.updateStatus() }
        }
        updateStatus()
    }

    @objc private func toggleConnection() {
        manager?.toggleConnection()
    }

    private func updateStatus() {
        guard let manager, let button = statusItem?.button else { return }
        let state = manager.isBusy ? "Working…" : (manager.isConnected ? "Connected" : "Disconnected")
        let action = manager.isConnected ? "Click to disconnect" : "Click to connect"
        var description = "\(manager.deviceName) — \(state)"
        if let error = manager.errorMessage {
            description += "\n\(error)"
        } else if !manager.hasDevice {
            description += "\nPair your AirPods in Bluetooth Settings first."
        } else if !manager.isBusy {
            description += "\n\(action)"
        }
        // Keep the white artwork undimmed while busy. The manager rejects extra
        // clicks until the operation finishes.
        button.toolTip = description
        button.setAccessibilityLabel("AirPodsLinker")
        button.setAccessibilityValue(description)
    }
}

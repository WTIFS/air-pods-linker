// swift-tools-version: 6.0
import PackageDescription

let package = Package(
    name: "AirPodsLinker",
    platforms: [.macOS(.v14)],
    targets: [
        .executableTarget(
            name: "AirPodsLinker",
            path: "Sources",
            linkerSettings: [
                .linkedFramework("IOBluetooth")
            ]
        ),
        .testTarget(name: "AirPodsLinkerTests", dependencies: ["AirPodsLinker"], path: "Tests")
    ]
)

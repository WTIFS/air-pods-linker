# AirPodsLinker

Small macOS menu-bar and Android apps to connect or disconnect already-paired AirPods.
Pair the headphones in the operating system's Bluetooth settings first. Initial discovery
matches names containing “AirPods” (case-insensitive). The saved paired device remains
selected after renaming; unpairing it allows another AirPods device to be selected.
When several devices match, the lowest Bluetooth address is selected deterministically.
There is currently no device picker.

## macOS

Requires macOS 14+ and Swift 6 (Xcode or Command Line Tools).

```sh
./mac/build.sh
open mac/AirPodsLinker.app
```

Allow Bluetooth access if prompted. Click the white AirPods menu-bar icon once to
connect or disconnect. There is no dropdown menu. Hover over the icon for connection
status or an error message. Repeated clicks are ignored while a request is running.
Status refreshes every three seconds; connection requests run in the background.
The icon stays pure white in both light and dark appearance. The displayed connection
is the Bluetooth link state, not a guarantee that macOS has selected the AirPods as
its audio output.

To launch at login, copy the app to `/Applications` and add that copy in System
Settings → General → Login Items. Existing login registration is preserved.
To quit this menu-less app, use Activity Monitor or `killall AirPodsLinker` in Terminal.

The build script ad-hoc signs the bundle for local use. Set `CODESIGN_IDENTITY` to a
Developer ID identity for a signed build; distributing it also requires notarization.

```sh
swift test --package-path mac
```

## Android

Requires Android 11+, JDK 17 or 21, and Android SDK 35. Set `ANDROID_HOME` to your SDK
or configure `sdk.dir` in `android/local.properties`.

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK: `android/app/build/outputs/apk/debug/app-debug.apk` (relative to the repository root).
Grant Nearby devices permission on Android 12+. If permission was permanently denied,
use the App Settings button to enable it. Scanning and location permissions are not needed.

**Android does not offer ordinary apps a public A2DP connect/disconnect API.** The app
makes a best-effort call to the device's hidden methods. Many Android versions/OEMs block
these methods or require privileged permissions. In those cases, use the Bluetooth
Settings button to connect/disconnect manually. A successful method return only means
the request was accepted: the app waits for actual profile state and times out after
15 seconds if the request remains unconfirmed. A2DP state describes media audio;
a separate headset/call profile may still be connected.

## Verification

Automated tests cover macOS saved-device selection and Android connection confirmation.
Builds and Android lint do not validate physical Bluetooth behavior. On real hardware,
check permission denial/grant, Bluetooth off/on, pairing/unpairing, out-of-range AirPods,
connections changed from system settings, repeated taps, and returning from Settings.
On macOS also check login-item approval; on Android check rotation and recreation.

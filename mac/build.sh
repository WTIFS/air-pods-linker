#!/bin/bash
set -euo pipefail

APP_NAME="AirPodsLinker"
BUNDLE="${APP_NAME}.app"

cd "$(dirname "$0")"

echo "==> Building ${APP_NAME} (release) …"
swift build -c release

echo "==> Assembling .app bundle …"
rm -rf "${BUNDLE}"

mkdir -p "${BUNDLE}/Contents/MacOS"
mkdir -p "${BUNDLE}/Contents/Resources"

cp ".build/release/${APP_NAME}" "${BUNDLE}/Contents/MacOS/"

cat > "${BUNDLE}/Contents/Info.plist" << 'PLIST'
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>AirPodsLinker</string>
    <key>CFBundleIdentifier</key>
    <string>com.wtifs.airpodslinker</string>
    <key>CFBundleName</key>
    <string>AirPodsLinker</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>1.0</string>
    <key>CFBundleVersion</key>
    <string>1</string>
    <key>LSMinimumSystemVersion</key>
    <string>14.0</string>
    <key>LSUIElement</key>
    <true/>
    <key>NSBluetoothAlwaysUsageDescription</key>
    <string>AirPodsLinker needs Bluetooth access to connect and disconnect your AirPods.</string>
    <key>NSHighResolutionCapable</key>
    <true/>
</dict>
</plist>
PLIST

# Sign the assembled bundle so macOS can validate its resources. For distribution,
# set CODESIGN_IDENTITY to a Developer ID identity and notarize separately.
codesign --force --sign "${CODESIGN_IDENTITY:--}" "${BUNDLE}"
codesign --verify --strict "${BUNDLE}"

echo ""
echo "✅  Done — ${BUNDLE}"
echo ""
echo "    Run once:   open ${BUNDLE}"
echo "    Permanent:  cp -R ${BUNDLE} /Applications/"
echo "                (required for Launch at Login to work)"

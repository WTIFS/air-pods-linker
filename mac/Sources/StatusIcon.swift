import AppKit

@MainActor
func makeStatusIcon() -> NSImage {
    let size = NSSize(width: 19, height: 19)
    let symbol = NSImage(systemSymbolName: "airpodspro", accessibilityDescription: nil)!
        .withSymbolConfiguration(.init(pointSize: 17, weight: .medium))!
    let image = NSImage(size: size, flipped: false) { rect in
        let scale = min(rect.width / symbol.size.width, rect.height / symbol.size.height)
        let symbolSize = NSSize(width: symbol.size.width * scale, height: symbol.size.height * scale)
        let symbolRect = NSRect(
            x: (rect.width - symbolSize.width) / 2,
            y: (rect.height - symbolSize.height) / 2,
            width: symbolSize.width, height: symbolSize.height
        )
        symbol.draw(in: symbolRect)
        NSColor.white.setFill()
        rect.fill(using: .sourceIn)
        return true
    }
    // A template image would turn black in light mode. Preserve literal white.
    image.isTemplate = false
    return image
}

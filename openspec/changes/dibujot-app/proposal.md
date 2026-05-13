# Proposal: DibujoT Android App

## Intent

Build the DibujoT app from scratch. The current repo is an empty "No Activity" Android shell. We need a working app that shows a gallery of 8 drawable images, lets the user pick one, and sends the matching `.gcode` file line-by-line to an Arduino via USB OTG serial.

## Scope

### In Scope
- Gallery screen with 8 images (RecyclerView + GridLayoutManager)
- DrawingItem model + hardcoded asset mapping (image → gcode)
- G-code file loading from assets and line-by-line parsing
- USB OTG serial layer (usb-serial-for-android, CH340/CP210x)
- G-code sender with ACK-gated line dispatch (coroutines)
- SendActivity + SendViewModel with sealed UiState feedback

### Out of Scope
- Dynamic image/gcode management (no file picker, no user uploads)
- Multi-device USB support or Bluetooth
- G-code editing or preview
- Cloud sync or persistence beyond in-memory state
- Init handshake beyond standard serial open

## Capabilities

### New Capabilities
- `gallery`: Image gallery screen — RecyclerView, GridLayoutManager, tap to select
- `drawing-data`: DrawingItem model, DrawingRepository, GcodeLoader interface + AssetGcodeLoader impl
- `gcode-parser`: Loads `.gcode` from assets, parses into line list, filters blanks/comments
- `usb-serial`: SerialPort interface + UsbSerialPort impl (usb-serial-for-android), FakeSerialPort test double, UsbPermissionHelper
- `gcode-sender`: GcodeSender coroutine engine — sends one line, waits for "ok\n" ACK, advances queue
- `send-ui`: SendActivity + SendViewModel with sealed UiState (Idle / Connecting / Ready / Sending / Done / Error)

### Modified Capabilities
None — this is a greenfield build; no existing specs to modify.

## Approach

Deliver in 4 phases to keep each PR reviewable and independently verifiable:

1. **Data + Gallery** — DrawingItem, DrawingRepository, GalleryActivity/Adapter. Pure UI, no hardware.
2. **G-code parsing** — GcodeParser (pure Kotlin, fully unit-tested), AssetGcodeLoader.
3. **USB Serial layer** — SerialPort interface + UsbSerialPort, UsbPermissionHelper. Integration-only (hardware required).
4. **Sender + Send UI** — GcodeSender (unit-tested via FakeSerialPort), SendViewModel, SendActivity.

The SerialPort interface is the critical testability boundary — all logic above it is unit-testable.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `app/src/main/java/edu/robotics/dibujot/data/` | New | DrawingItem, DrawingRepository, GcodeLoader, AssetGcodeLoader |
| `app/src/main/java/edu/robotics/dibujot/serial/` | New | SerialPort interface, UsbSerialPort, FakeSerialPort |
| `app/src/main/java/edu/robotics/dibujot/gcode/` | New | GcodeParser, GcodeSender |
| `app/src/main/java/edu/robotics/dibujot/ui/gallery/` | New | GalleryActivity, GalleryAdapter |
| `app/src/main/java/edu/robotics/dibujot/ui/send/` | New | SendActivity, SendViewModel, UiState |
| `app/src/main/java/edu/robotics/dibujot/util/` | New | UsbPermissionHelper |
| `app/src/main/res/drawable/` | New | 8 image thumbnails |
| `app/src/main/assets/gcode/` | New | 8 `.gcode` files |
| `app/build.gradle.kts` | Modified | Add usb-serial, coroutines, lifecycle deps |
| `app/src/main/AndroidManifest.xml` | Modified | USB host feature, permission, activity declarations |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Arduino ACK format differs from "ok\n" | Low | Open assumption; GcodeSender accepts configurable ACK string |
| USB permission denied or device not found | Med | UsbPermissionHelper handles runtime permission; UiState.Error surfaces failures |
| CH340/CP210x not detected by library | Low | usb-serial-for-android supports both; test on target hardware |
| Coroutine ACK timeout hangs UI | Med | Add timeout to ACK wait; transition to UiState.Error on timeout |

## Rollback Plan

This is a greenfield build. Rollback = revert the feature branch. No existing functionality is touched; the empty shell remains intact on main. Each of the 4 delivery phases can be reverted independently via PR revert.

## Dependencies

- `usb-serial-for-android 3.7.0` (JitPack) — USB OTG serial communication
- `kotlinx-coroutines-android 1.7.3` — async G-code sending
- `kotlinx-coroutines-test 1.7.3` — testing coroutine flows
- `lifecycle-viewmodel-ktx 2.6.2` — ViewModel + viewModelScope
- `lifecycle-livedata-ktx 2.6.2` — LiveData / StateFlow

## Success Criteria

- [ ] Gallery shows 8 images; tapping one navigates to SendActivity with correct DrawingItem
- [ ] GcodeParser correctly parses all 8 `.gcode` files (unit tests pass)
- [ ] USB connect → UiState transitions Idle → Connecting → Ready (no crash)
- [ ] G-code send completes all lines and reaches UiState.Done on hardware
- [ ] FakeSerialPort unit tests cover GcodeSender line dispatch and ACK gating
- [ ] `./gradlew assembleDebug` succeeds cleanly

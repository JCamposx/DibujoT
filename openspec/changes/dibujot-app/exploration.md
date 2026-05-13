# Exploration: DibujoT — Full Feature Scope

**Change name**: `dibujot-app`  
**Date**: 2026-05-13  
**Phase**: explore

---

## Current State

The project is a blank Android "No Activity" Kotlin shell (AGP 9.2.1, minSdk 24, targetSdk 36).  
No source files exist yet (`app/src/main/java/com/` is empty). No assets. No activities.  
Dependencies: `appcompat`, `core-ktx`, `material`, JUnit4, Espresso.  
Manifest declares no activities, no permissions, no USB intent filters.

The full app must be built from scratch. This exploration defines what to build and how.

---

## Feature Areas

### 1. Gallery Screen — Display 8 images, pick one

**Requirement**: Show thumbnails for all 8 drawings. User taps one to proceed to send screen.

**Approach options**:

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| RecyclerView + GridLayoutManager | Flexible, standard, testable with Espresso | More boilerplate than simple list | Low |
| ListView + ArrayAdapter | Simpler code | Deprecated pattern, less flexible | Low |
| Jetpack Compose LazyVerticalGrid | Modern, declarative, easiest to test with Compose test rules | Requires adding Compose deps (~3 libs) | Medium |

**Recommendation**: `RecyclerView + GridLayoutManager` — stays within existing `appcompat` + `material` deps, patterns are well-known, Espresso can validate item count and click behavior.

**Testability**: UI tested via Espresso `onView(withId(R.id.gallery_rv)).check(matches(hasChildCount(8)))`. Grid item click → navigation tested with ActivityScenario.

---

### 2. Image-to-GCode Mapping — Bundle and map pairs as assets

**Requirement**: 8 image + .gcode pairs must be bundled in the app and associated by a known key.

**Options**:

| Approach | Pros | Cons | Effort |
|---|---|---|---|
| Hardcoded map in Kotlin data class | Zero runtime overhead, compile-time safe | Manual maintenance | Low |
| JSON manifest in `assets/` | Flexible, decoupled | Parsing logic, file I/O at startup | Medium |
| File name convention (`img_01.png` → `gcode_01.gcode`) | Zero config needed | Brittle if naming drifts | Low |

**Recommendation**: **Hardcoded `List<DrawingItem>` data class** — the set is fixed (8 items, academic project), no dynamic loading needed. Define a `data class DrawingItem(val id: Int, val imageResId: Int, val gcodeAssetPath: String)`. Store images in `res/drawable/` (thumbnails) and gcode files in `assets/gcode/`.

**Asset layout**:
```
app/src/main/assets/gcode/drawing_01.gcode
app/src/main/assets/gcode/drawing_02.gcode
... (x8)
app/src/main/res/drawable/drawing_01.png
... (x8)
```

**Testability**: `DrawingRepository` (pure Kotlin, no Android deps) returns the hardcoded list — fully unit-testable with JUnit4. Asset reading (`AssetManager.open()`) is wrapped behind an interface `GcodeLoader` so it can be faked in unit tests.

---

### 3. USB Serial Layer — OTG connection to Arduino, line-by-line with ACK wait

**Requirement**: Connect to Arduino via USB OTG, send one G-code line at a time, wait for response before sending next.

**Hardware context**: Custom Arduino firmware, NOT GRBL. Response format TBD (likely `"ok\n"` or similar). USB OTG = Android as USB Host.

**Android USB Host API fundamentals**:
- `UsbManager` → enumerate devices → `UsbDevice`
- Request permission via `PendingIntent`
- Open `UsbDeviceConnection` + `UsbInterface` + `UsbEndpoint` (bulk OUT + bulk IN)
- Communication is raw byte transfer via `bulkTransfer()`

**Third-party serial lib options**:

| Library | Pros | Cons |
|---|---|---|
| [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) | Mature, abstracts chip detection (CH340, CP210x, FTDI), widely used | External dep, adds 50KB |
| Raw Android USB Host API | No dep | Must handle chip-specific protocol yourself (baud, stop bits, parity) |

**Recommendation**: **`usb-serial-for-android`** — Arduino boards use CH340 or CP210x USB-UART bridges; handling these manually is error-prone and out of scope for an academic project. The library wraps `UsbDeviceConnection` cleanly.

**Interface design** (critical for testability):
```kotlin
interface SerialPort {
    fun connect(device: UsbDevice): Result<Unit>
    fun disconnect()
    fun writeLine(line: String)
    fun readLine(): String?
    val isConnected: Boolean
}
```

`UsbSerialPort` = real implementation using `usb-serial-for-android`.  
`FakeSerialPort` = test double used in unit tests (no hardware required).

**USB OTG permission flow**:
- Register `BroadcastReceiver` for `UsbManager.ACTION_USB_DEVICE_ATTACHED`
- Request permission with `UsbManager.requestPermission()`
- Handle `ACTION_USB_DEVICE_DETACHED` for disconnect/cleanup

**Testability**:  
- `SerialPort` interface → unit-testable sender logic via `FakeSerialPort`  
- Real USB path requires a physical device → instrumented test only, explicitly excluded from unit suite  
- Permission + lifecycle tests: Espresso + ActivityScenario with mocked `UsbManager`

---

### 4. G-code Sender — Load file, parse lines, send queue with ACK

**Requirement**: Read a `.gcode` asset, parse non-empty/non-comment lines, send one by one, wait for Arduino ACK before next.

**G-code line format**:  
- Skip blank lines  
- Skip comment lines starting with `;`  
- Each valid line sent as-is with `\n` appended (or `\r\n` — to confirm with partner)

**Sender design**:
```kotlin
class GcodeSender(
    private val port: SerialPort,
    private val lines: List<String>
) {
    suspend fun sendAll(onProgress: (sent: Int, total: Int) -> Unit)
    fun cancel()
}
```

- Runs in a coroutine (off main thread)
- After each `writeLine()`, waits for `readLine()` response before continuing
- Emits progress events consumed by UI
- Supports cancellation

**State machine** (simplified):
```
IDLE → CONNECTING → READY → SENDING(n/total) → DONE / ERROR / CANCELLED
```

**Testability**:
- `GcodeParser` (pure Kotlin) — fully unit-testable: filters blanks, strips comments, normalizes terminators
- `GcodeSender` — unit-testable with `FakeSerialPort` that auto-replies `"ok"`
- Progress callback testable synchronously in JUnit4

---

### 5. Connection Status / Feedback UI

**Requirement**: User needs to see: USB connected/disconnected, sending progress (line N of M), success/error state.

**UI elements needed**:
- Status indicator (icon + text): `Disconnected` / `Connected` / `Sending...` / `Done` / `Error`
- Progress bar or counter: `Sending line 12 / 47`
- Cancel button (visible during sending)
- Error message area

**Screen flow**:
```
GalleryActivity → [tap image] → SendActivity
  SendActivity:
    - Shows selected image (confirmation)
    - "Connect" button (or auto-connects on attach)
    - Status + progress display
    - "Send" button (enabled only when connected)
    - "Cancel" button (visible while sending)
```

**Architecture recommendation**: ViewModel + LiveData (or StateFlow) pattern.
- `SendViewModel` holds `UiState` (sealed class: `Idle`, `Connecting`, `Ready`, `Sending(n, total)`, `Done`, `Error(msg)`)
- Activity observes and renders state
- No logic in Activity/Fragment

**Testability**:
- `SendViewModel` unit-testable with `FakeSerialPort` and `TestCoroutineDispatcher`
- UI states tested with Espresso: verify button enabled/disabled states per `UiState`

---

## Architecture Overview

```
com.example.dibujot
├── data/
│   ├── DrawingItem.kt              // data class
│   ├── DrawingRepository.kt        // hardcoded list of 8
│   └── GcodeLoader.kt              // interface + AssetGcodeLoader impl
├── serial/
│   ├── SerialPort.kt               // interface
│   ├── UsbSerialPort.kt            // real impl (usb-serial-for-android)
│   └── FakeSerialPort.kt           // test double (in test/ tree)
├── gcode/
│   ├── GcodeParser.kt              // pure Kotlin, no Android deps
│   └── GcodeSender.kt              // coroutine-based sender
├── ui/
│   ├── gallery/
│   │   ├── GalleryActivity.kt
│   │   └── GalleryAdapter.kt
│   └── send/
│       ├── SendActivity.kt
│       └── SendViewModel.kt
└── util/
    └── UsbPermissionHelper.kt      // USB lifecycle + permission
```

---

## Affected Areas

All files are **new** — no existing source to modify:
- `app/src/main/java/com/example/dibujot/` — all source
- `app/src/main/assets/gcode/` — 8 gcode files to add
- `app/src/main/res/drawable/` — 8 thumbnail images
- `app/src/main/AndroidManifest.xml` — add activities, USB intent filters, permissions
- `app/build.gradle.kts` — add `usb-serial-for-android` dependency, Kotlin coroutines
- `gradle/libs.versions.toml` — add version entries

---

## Key Risks

1. **Arduino response format unknown** — `GcodeSender` must be designed to accept a configurable response matcher. Default to `line.trim() == "ok"` but make it injectable.
2. **USB OTG not testable on JVM** — the `SerialPort` interface boundary is the ONLY mitigation. All logic above it must be tested without hardware.
3. **USB permission denied or device not found** — must handle gracefully in `UsbPermissionHelper` and surface to `UiState.Error`.
4. **Baud rate / stop bits unknown** — typical for Arduino USB-UART: 9600 or 115200 baud, 8N1. Must confirm with partner before implementation.
5. **G-code line terminator** — `\n` vs `\r\n` to confirm. Arduino firmware may be picky.
6. **No Kotlin Coroutines in deps yet** — must add `kotlinx-coroutines-android` to build.gradle.

---

## Dependencies to Add

```toml
# gradle/libs.versions.toml
usbSerial = "3.7.0"
coroutines = "1.7.3"
lifecycleViewModel = "2.6.2"

[libraries]
usb-serial = { group = "com.github.mik3y", name = "usb-serial-for-android", version.ref = "usbSerial" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycleViewModel" }
lifecycle-livedata-ktx = { group = "androidx.lifecycle", name = "lifecycle-livedata-ktx", version.ref = "lifecycleViewModel" }
```

Also requires JitPack in `settings.gradle.kts` for `usb-serial-for-android`.

---

## Open Questions (Blocking for Spec)

| # | Question | Impact |
|---|---|---|
| Q1 | What does the Arduino firmware respond to each G-code line? (`"ok"`, `"OK\r\n"`, other?) | `GcodeSender` ACK matcher |
| Q2 | Baud rate? (9600 / 115200?) | `UsbSerialPort` config |
| Q3 | Line terminator the firmware expects? (`\n` or `\r\n`?) | `GcodeParser` output |
| Q4 | Does the Arduino need any handshake/init before G-code lines start? | `GcodeSender` startup sequence |

Q1–Q4 are **non-blocking for architecture** but **blocking for the serial spec**. Implementation can proceed with injectable defaults and a config object.

---

## Recommendation

Proceed with a 4-phase delivery:

1. **Phase 1 — Data + Gallery**: `DrawingItem`, `DrawingRepository`, `GalleryActivity` + adapter, 8 image assets. Pure UI, no serial.
2. **Phase 2 — G-code parsing**: `GcodeParser`, `GcodeLoader` interface + asset impl. Pure Kotlin, fully unit-testable.
3. **Phase 3 — USB Serial layer**: `SerialPort` interface, `UsbSerialPort`, permission flow. Integration-only (hardware).
4. **Phase 4 — Sender + UI**: `GcodeSender`, `SendViewModel`, `SendActivity`. Unit-tested with `FakeSerialPort`.

Each phase is independently deliverable and reviewable.

---

## Ready for Proposal

**Yes** — with the caveat that Q1–Q4 should be captured as open assumptions in the spec. Architecture is fully defined, interfaces are stable, testability strategy is clear.

# Design: DibujoT Android App

## Technical Approach

Greenfield build on an empty Android shell. Layered architecture with a hard testability boundary at `SerialPort`. Pure-Kotlin layers (data, gcode) are fully unit-testable; the USB layer is integration-only. ViewModel + sealed `UiState` drives the Send screen reactively via LiveData/StateFlow.

## Architecture Decisions

| Decision | Choice | Rejected | Rationale |
|----------|--------|----------|-----------|
| USB serial lib | `usb-serial-for-android` via JitPack | Raw `UsbManager` API | Abstracts CH340/CP210x chip detection; saves ~300 lines of driver code |
| Testability wall | `SerialPort` interface | Direct `UsbSerialPort` usage | USB OTG is untestable on JVM; `FakeSerialPort` makes `GcodeSender` fully unit-testable |
| Async model | Coroutines (`suspend`) | Callbacks / `HandlerThread` | Structured concurrency; sequential send→ACK→send reads linearly; easy timeout with `withTimeout` |
| UI state | `sealed class UiState` + LiveData | Bare `LiveData<String>` | Exhaustive state enum prevents undefined transitions; compiler-enforced `when` branches |
| Data source | Hardcoded `DrawingRepository` | SQLite / JSON config | Scope is 8 fixed items; DB adds zero value and slows Phase 1 |
| Asset storage | `assets/gcode/` | `res/raw/` | `assets/` supports subdirectories and arbitrary filenames; accessed via `AssetManager` |

## Data Flow

```
GalleryActivity ──tap──► SendActivity
                              │
                    SendViewModel.startSend(item)
                              │
                   GcodeParser.parse(item.gcodeAssetPath)
                              │  List<String>
                   GcodeSender.send(lines, serialPort)
                         │              │
                  SerialPort.send()  SerialPort.readLine()
                         │              │
                  UsbSerialPort   ◄──── Arduino ACK "ok\n"
                         │
                   UiState (Idle → Connecting → Ready → Sending(n/total) → Done | Error)
                         │
                    SendActivity (observes LiveData<UiState>)
```

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `data/DrawingItem.kt` | Create | Data class: `id`, `imageResId`, `gcodeAssetPath` |
| `data/DrawingRepository.kt` | Create | `fun getAll(): List<DrawingItem>` — hardcoded 8 items |
| `data/GcodeLoader.kt` | Create | Interface + `AssetGcodeLoader` impl using `AssetManager` |
| `serial/SerialPort.kt` | Create | Interface: `connect()`, `send(line)`, `readLine(): String`, `disconnect()` |
| `serial/UsbSerialPort.kt` | Create | `usb-serial-for-android` impl; requests USB permission via `UsbPermissionHelper` |
| `serial/FakeSerialPort.kt` | Create | Test double: queued replies, records sent lines |
| `gcode/GcodeParser.kt` | Create | `fun parse(assetPath: String): List<String>` — filters blank/comment lines |
| `gcode/GcodeSender.kt` | Create | `suspend fun send(lines, port, onProgress)` — ACK-gated loop with `withTimeout` |
| `ui/GalleryActivity.kt` | Create | RecyclerView + GridLayoutManager(2); navigates to SendActivity |
| `ui/GalleryAdapter.kt` | Create | `ListAdapter<DrawingItem>`; uses DiffUtil |
| `ui/SendActivity.kt` | Create | Observes `SendViewModel.uiState`; shows progress + error |
| `ui/SendViewModel.kt` | Create | `startSend(item)`, `UiState` LiveData, injects `SerialPort` |
| `util/UsbPermissionHelper.kt` | Create | Wraps `UsbManager` permission broadcast flow |
| `app/build.gradle.kts` | Modify | Add JitPack repo + `usb-serial-for-android`, coroutines, lifecycle deps |
| `app/src/main/AndroidManifest.xml` | Modify | USB host feature, USB permission, activity declarations |
| `app/src/main/assets/gcode/` | Create | 8 `.gcode` files |
| `app/src/main/res/drawable/` | Create | 8 image thumbnails |

## Interfaces / Contracts

```kotlin
// serial/SerialPort.kt
interface SerialPort {
    fun connect(baudRate: Int = 115200)
    fun send(line: String)          // appends "\n" if absent
    fun readLine(): String          // blocks until "\n" received
    fun disconnect()
}

// gcode/GcodeSender.kt
suspend fun send(
    lines: List<String>,
    port: SerialPort,
    ack: String = "ok",
    timeoutMs: Long = 5_000,
    onProgress: (sent: Int, total: Int) -> Unit
)

// ui/SendViewModel.kt — sealed UiState
sealed class UiState {
    object Idle : UiState()
    object Connecting : UiState()
    object Ready : UiState()
    data class Sending(val sent: Int, val total: Int) : UiState()
    object Done : UiState()
    data class Error(val message: String) : UiState()
}
```

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit | `GcodeParser` | JUnit4; assert filtered lines, blank/comment removal |
| Unit | `GcodeSender` | JUnit4 + coroutines-test; `FakeSerialPort` queues "ok" replies; verify line order and timeout |
| Unit | `DrawingRepository` | Assert 8 items, all asset paths non-null |
| Unit | `SendViewModel` | `InstantTaskExecutorRule`; mock `GcodeSender`; verify state transitions |
| Integration | `UsbSerialPort` | Manual / hardware-only; no JVM path |
| E2E | Full send flow | Espresso + hardware Arduino (acceptance, not CI) |

## Migration / Rollout

No migration required. Greenfield build delivered in 4 independent PR phases:
1. Data + Gallery
2. G-code parser
3. USB serial layer
4. GcodeSender + Send UI

Each phase is independently revertable.

## Open Questions

- [ ] Arduino ACK format — assumed `"ok\n"`; confirm with firmware source
- [ ] Baud rate — assumed `115200`; confirm Arduino sketch config
- [ ] Line terminator — assumed `\n`; confirm GRBL/firmware setting
- [ ] Init handshake — assumed none; confirm no `$$` or `~` required on connect

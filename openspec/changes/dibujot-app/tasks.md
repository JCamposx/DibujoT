# Tasks: DibujoT Android App

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 900–1 200 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Data + Gallery → PR 2: GcodeParser → PR 3: USB Serial → PR 4: GcodeSender + SendUI |
| Delivery strategy | stacked-to-main |
| Chain strategy | stacked-to-main |

Decision needed before apply: Resolved (stacked-to-main, Slice 1 autonomous)
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Data layer + Gallery screen | PR 1 | Base: main |
| 2 | G-code parser | PR 2 | Base: PR 1 branch |
| 3 | USB serial layer | PR 3 | Base: PR 2 branch |
| 4 | GcodeSender + Send UI | PR 4 | Base: PR 3 branch |

---

## Phase 1: Foundation — Build Config & Assets

- [x] 1.1 `app/build.gradle.kts` — add JitPack repo + `usb-serial-for-android`, recyclerview, mockito, robolectric
- [x] 1.2 `app/src/main/AndroidManifest.xml` — declare USB host `<uses-feature>`, USB permission, GalleryActivity (launcher), SendActivity
- [x] 1.3 `app/src/main/assets/gcode/` — add 8 placeholder `.gcode` files (one per drawing)
- [x] 1.4 `app/src/main/res/drawable/` — add 8 placeholder drawable XML files
- [x] 1.5 Verify `./gradlew assembleDebug` succeeds — ✅ BUILD SUCCESSFUL

## Phase 2: Data Layer (TDD — RED → GREEN → REFACTOR)

- [x] 2.1 **RED** — Write `DrawingItemTest`: Parcelable round-trip preserves all fields
- [x] 2.2 **RED** — Write `DrawingRepositoryTest`: `getAll()` returns exactly 8 items with non-null fields
- [x] 2.3 **RED** — Write `AssetGcodeLoaderTest`: existing asset loaded; missing asset throws `IOException`
- [x] 2.4 **GREEN** — Create `data/DrawingItem.kt` (Parcelable data class: `id`, `imageResId`, `gcodeAssetPath`)
- [x] 2.5 **GREEN** — Create `data/DrawingRepository.kt` — hardcoded 8 `DrawingItem` instances
- [x] 2.6 **GREEN** — Create `data/GcodeLoader.kt` — interface + `AssetGcodeLoader` impl using `AssetManager`
- [x] 2.7 **REFACTOR** — All data tests pass; clean up any duplication
- [x] 2.8 Verify `./gradlew test` green — ✅ BUILD SUCCESSFUL

## Phase 3: Gallery Screen (TDD — RED → GREEN → REFACTOR)

- [x] 3.1 **RED** — Write `GalleryAdapterTest`: adapter item count matches repo; stable IDs
- [x] 3.2 **GREEN** — Create `ui/gallery/GalleryAdapter.kt` (`ListAdapter<DrawingItem>` + `DiffUtil`)
- [x] 3.3 **GREEN** — Create `ui/gallery/GalleryActivity.kt` — RecyclerView + `GridLayoutManager(2)`; on item tap start `SendActivity` with `DrawingItem` extra
- [x] 3.4 **REFACTOR** — Gallery tests pass; constants extracted
- [x] 3.5 Verify `./gradlew test` green — ✅ BUILD SUCCESSFUL

## Phase 4: G-Code Parser (TDD — RED → GREEN → REFACTOR)

- [x] 4.1 **RED** — Write `GcodeParserTest` covering all spec scenarios: blanks removed, comment lines removed, inline comments stripped, valid commands preserved, empty input → empty output, order preserved
- [x] 4.2 **GREEN** — Create `gcode/GcodeParser.kt` — `fun parse(lines: List<String>): List<String>`
- [x] 4.3 **REFACTOR** — All parser tests pass; no duplication
- [x] 4.4 Verify `./gradlew test` green — ✅ BUILD SUCCESSFUL

## Phase 5: USB Serial Layer (TDD — RED → GREEN → REFACTOR)

- [x] 5.1 **RED** — Write `FakeSerialPortTest`: programmed responses returned in order; written bytes captured
- [x] 5.2 **GREEN** — Create `serial/SerialPort.kt` — interface (`connect`, `send`, `readLine`, `disconnect`)
- [x] 5.3 **GREEN** — Create `serial/FakeSerialPort.kt` — test double with queued replies + capture list
- [x] 5.4 **GREEN** — Create `serial/UsbSerialPort.kt` — `usb-serial-for-android` impl; opens first CH340/CP210x at 115200 8N1
- [x] 5.5 **GREEN** — Create `util/UsbPermissionHelper.kt` — wraps `UsbManager` broadcast; invokes `callback(granted: Boolean)`
- [x] 5.6 **REFACTOR** — All serial tests pass; interface matches design contract exactly
- [x] 5.7 Verify `./gradlew test` green — ✅ BUILD SUCCESSFUL

## Phase 6: GcodeSender (TDD — RED → GREEN → REFACTOR)

- [x] 6.1 **RED** — Write `GcodeSenderTest` via `FakeSerialPort`: N lines sent and ACKed → completes; each line triggers exactly one `readLine`; custom ACK accepted; timeout → `TimeoutCancellationException`; progress emissions `(1,3),(2,3),(3,3)` for 3 lines
- [x] 6.2 **GREEN** — Create `gcode/GcodeSender.kt` — `suspend fun send(lines, port, ack, timeoutMs, onProgress)` with `withTimeout` ACK loop
- [x] 6.3 **REFACTOR** — All sender tests pass
- [x] 6.4 Verify `./gradlew test` green ✅

## Phase 7: Send UI (TDD — RED → GREEN → REFACTOR)

- [x] 7.1 **RED** — Write `SendViewModelTest` (coroutines-test): connect → Ready; USB failure → Error; send all lines → Done; ACK timeout → Error with message
- [x] 7.2 **GREEN** — Create `ui/send/SendViewModel.kt` — `startSend(lines)`, `connect(port)`, `UiState` StateFlow, injects `SerialPort`
- [x] 7.3 **GREEN** — Create `ui/send/SendActivity.kt` — observes `uiState`; shows progress string `"n/total"`, error text, Done state disables send button
- [x] 7.4 **REFACTOR** — All ViewModel tests pass; Activity uses `findViewById` consistently
- [x] 7.5 Verify `./gradlew test` green ✅; `./gradlew assembleDebug` clean ✅

## Phase 8: Wiring & Cleanup

- [x] 8.1 Wire `GalleryActivity` → `SendActivity` intent extras match `DrawingItem` Parcelable key ✅ (already done in Phase 3)
- [x] 8.2 Wire `SendActivity` reads `DrawingItem` from intent and calls `viewModel.startSend(parsed)` after connect
- [x] 8.3 Wire `AssetGcodeLoader` and `UsbSerialPort` into `SendActivity` → `SendViewModel`
- [x] 8.4 ACK format: `"ok\n"` (configurable), baud 115200, line terminator `\n`, no init handshake
- [x] 8.5 Remove TODO/placeholder comments ✅
- [x] 8.6 Final `./gradlew test` ✅ BUILD SUCCESSFUL; `./gradlew assembleDebug` ✅ BUILD SUCCESSFUL

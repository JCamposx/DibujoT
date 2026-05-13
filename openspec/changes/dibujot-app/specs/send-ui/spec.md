# Send UI Specification

## Purpose

SendActivity and SendViewModel providing user-facing feedback for the USB connect → send → done flow, driven by a sealed UiState.

## Requirements

### Requirement: Sealed UiState

SendViewModel MUST expose a sealed UiState with states: Idle, Connecting, Ready, Sending(linesSent: Int, total: Int), Done, Error(message: String).

#### Scenario: State transitions are type-safe

- GIVEN the ViewModel emits a UiState
- WHEN the Activity observes it
- THEN each `when` branch on the sealed class compiles without an `else` fallback

---

### Requirement: Connect Flow Updates State

Initiating a connection MUST transition UiState: Idle → Connecting → Ready (on success) or Error (on failure).

#### Scenario: Successful connect

- GIVEN UiState is Idle
- WHEN `connect()` is called and USB opens successfully
- THEN UiState transitions to Connecting then Ready

#### Scenario: USB open failure

- GIVEN UiState is Idle
- WHEN `connect()` is called and UsbSerialPort throws
- THEN UiState transitions to Error with a non-empty message

---

### Requirement: Send Flow Updates State

Starting a send MUST transition from Ready → Sending(n, total) per line → Done, or → Error on failure.

#### Scenario: Successful send to Done

- GIVEN UiState is Ready
- WHEN `startSend()` is called and all lines ACK
- THEN UiState progresses through Sending states and ends at Done

#### Scenario: Sender timeout → Error

- GIVEN UiState is Ready
- WHEN `startSend()` is called and ACK times out
- THEN UiState transitions to Error with a descriptive message

---

### Requirement: SendActivity Reflects UiState

SendActivity MUST observe UiState LiveData and update UI elements for each state.

#### Scenario: Sending state shows progress

- GIVEN UiState is Sending(3, 10)
- WHEN the Activity receives this state
- THEN a progress indicator showing "3/10" (or equivalent) is visible

#### Scenario: Error state shows message

- GIVEN UiState is Error("Device not found")
- WHEN the Activity receives this state
- THEN "Device not found" is displayed to the user

#### Scenario: Done state shows completion

- GIVEN UiState is Done
- WHEN the Activity receives this state
- THEN a completion indicator is shown and the send button is disabled or hidden

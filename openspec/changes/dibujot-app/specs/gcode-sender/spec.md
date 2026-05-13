# GCode Sender Specification

## Purpose

Coroutine-based engine that sends parsed gcode lines one at a time over SerialPort, waiting for an ACK after each line before advancing.

## Requirements

### Requirement: Line-by-Line ACK-Gated Dispatch

GcodeSender MUST send each line followed by a newline, then wait for the ACK string before sending the next line.

#### Scenario: All lines sent and ACKed

- GIVEN a FakeSerialPort programmed with N "ok\n" responses
- WHEN `send(lines)` is called with N lines
- THEN all N lines are written to the port in order
- AND the function completes successfully

#### Scenario: Each line triggers one read

- GIVEN 3 lines and 3 ACK responses
- WHEN `send(lines)` completes
- THEN `write` was called 3 times and `readLine` was called 3 times

---

### Requirement: ACK String Is Configurable

GcodeSender MUST accept a configurable ACK string (default `"ok\n"`).

#### Scenario: Custom ACK accepted

- GIVEN GcodeSender constructed with ack = `"OK\r\n"`
- WHEN the port returns `"OK\r\n"` after each write
- THEN all lines complete successfully

---

### Requirement: ACK Timeout Results in Error

If no ACK is received within the configured timeout, GcodeSender MUST cancel the send and report an error.

#### Scenario: Timeout triggers error

- GIVEN a FakeSerialPort that never returns an ACK
- WHEN `send(lines)` is called with a 500ms timeout
- THEN the coroutine throws a TimeoutCancellationException (or equivalent)
- AND no further lines are sent after the first

---

### Requirement: Progress Reporting

GcodeSender MUST emit `(linesSent: Int, total: Int)` progress after each ACKed line via a callback or Flow.

#### Scenario: Progress increments per line

- GIVEN 3 lines and 3 ACK responses
- WHEN `send(lines)` is running
- THEN progress emissions are (1,3), (2,3), (3,3) in order

# USB Serial Specification

## Purpose

Testable serial communication layer for USB OTG to Arduino. Defines the interface boundary, real implementation, test double, and permission helper.

## Requirements

### Requirement: SerialPort Interface

SerialPort MUST define `open()`, `close()`, `write(data: ByteArray)`, and `readLine(): String` operations. It MUST be closeable (implement `Closeable`).

#### Scenario: Interface is the only dependency of GcodeSender

- GIVEN GcodeSender depends only on SerialPort
- WHEN a FakeSerialPort is injected
- THEN GcodeSender operates without any Android USB APIs

---

### Requirement: UsbSerialPort Opens Device

UsbSerialPort MUST open the first detected USB serial device (CH340 or CP210x) using usb-serial-for-android and configure baud rate 115200, 8N1.

#### Scenario: Device present and permission granted

- GIVEN a USB serial device is attached and permission was granted
- WHEN `open()` is called
- THEN the port is open and ready for write/read

#### Scenario: No device found

- GIVEN no USB serial device is attached
- WHEN `open()` is called
- THEN an IOException is thrown with a descriptive message

---

### Requirement: FakeSerialPort Test Double

FakeSerialPort MUST implement SerialPort and allow tests to pre-program read responses and capture written bytes.

#### Scenario: Programmed responses returned in order

- GIVEN FakeSerialPort is constructed with responses `["ok\n", "ok\n"]`
- WHEN `readLine()` is called twice
- THEN it returns `"ok\n"` both times

#### Scenario: Written bytes are captured

- GIVEN a FakeSerialPort instance
- WHEN `write(data)` is called
- THEN `writtenBytes` contains `data`

---

### Requirement: UsbPermissionHelper Requests Permission

UsbPermissionHelper MUST request Android USB permission for the target device and invoke a callback when granted or denied.

#### Scenario: Permission granted

- GIVEN the UsbDevice exists and the user approves the dialog
- WHEN `requestPermission(device, callback)` is called
- THEN `callback(true)` is invoked

#### Scenario: Permission denied

- GIVEN the user dismisses or denies the dialog
- WHEN `requestPermission(device, callback)` is called
- THEN `callback(false)` is invoked

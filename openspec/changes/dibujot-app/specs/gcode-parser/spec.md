# GCode Parser Specification

## Purpose

Pure Kotlin component that takes raw gcode lines and produces a clean, ordered list ready for serial dispatch.

## Requirements

### Requirement: Parse Raw Lines

GcodeParser MUST accept a `List<String>` of raw lines and return a filtered `List<String>`.

#### Scenario: Blanks removed

- GIVEN a list containing blank and whitespace-only strings
- WHEN `parse(lines)` is called
- THEN the returned list contains no blank or whitespace-only entries

#### Scenario: Comments removed

- GIVEN a list containing lines that start with `;`
- WHEN `parse(lines)` is called
- THEN lines beginning with `;` are absent from the result

#### Scenario: Inline comments stripped

- GIVEN a line like `G1 X10 ; move x`
- WHEN `parse(lines)` is called
- THEN the result contains `G1 X10` with no trailing comment

#### Scenario: Valid commands preserved

- GIVEN lines like `G28`, `G1 X0 Y0`, `M104 S200`
- WHEN `parse(lines)` is called
- THEN all three lines appear in the result in original order

#### Scenario: Empty input

- GIVEN an empty list
- WHEN `parse(lines)` is called
- THEN an empty list is returned

---

### Requirement: Order Preserved

GcodeParser MUST preserve the relative order of all non-filtered lines.

#### Scenario: Order maintained

- GIVEN a mixed list of valid lines and blanks/comments
- WHEN `parse(lines)` is called
- THEN valid lines appear in the same relative order as in the input

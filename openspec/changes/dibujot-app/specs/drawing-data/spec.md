# Drawing Data Specification

## Purpose

Data model and repository layer mapping 8 drawings (image resource → gcode asset path). Provides the single source of truth for the gallery and sender.

## Requirements

### Requirement: DrawingItem Model

DrawingItem MUST hold a display name, a drawable resource ID, and a gcode asset path. It MUST be Parcelable to survive Intent transport.

#### Scenario: Parcelable round-trip

- GIVEN a DrawingItem with name, drawableRes, and gcodeAsset fields set
- WHEN it is written to and read from a Parcel
- THEN all three fields match the original values

---

### Requirement: DrawingRepository Provides All Items

DrawingRepository MUST return a list of exactly 8 DrawingItems with non-null, non-empty fields.

#### Scenario: All 8 items returned

- GIVEN a DrawingRepository instance
- WHEN `getAll()` is called
- THEN a list of exactly 8 items is returned
- AND each item has a non-empty name, a valid drawableRes > 0, and a non-empty gcodeAsset path

---

### Requirement: GcodeLoader Interface

GcodeLoader MUST define a single `load(assetPath: String): List<String>` method that returns the raw lines of a gcode file.

#### Scenario: Interface contract is defined

- GIVEN any class implementing GcodeLoader
- WHEN `load(assetPath)` is called
- THEN a non-null List<String> is returned

---

### Requirement: AssetGcodeLoader Reads from Assets

AssetGcodeLoader MUST implement GcodeLoader by reading the given path from the Android assets folder and returning all lines.

#### Scenario: Existing asset is loaded

- GIVEN a valid gcode asset path exists under `assets/gcode/`
- WHEN `load(assetPath)` is called
- THEN the returned list contains all lines from the file

#### Scenario: Missing asset throws

- GIVEN a path that does not exist in assets
- WHEN `load(assetPath)` is called
- THEN an IOException (or subclass) is thrown

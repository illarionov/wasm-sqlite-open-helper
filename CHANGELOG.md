# Change Log

## [0.1-beta03] — 2025-04-12

### 🚀 New Feature

- Update Chasm to 0.9.65
- Update Chicory to 1.2.1

### 🤖 Dependencies

- Bump androidx-sqlite to 2.5.0
- Bump Kotlin to 2.1.20
- Bump Wasi-emscripten-host to 0.5
- Bump other build dependencies

## [0.1-beta02] — 2025-03-19

### Added

- Expose Chicory memory factory and use ExactMemAllocStrategy

### 🐛 Bug Fix

- Fix memory leak in `sqlite3_free()`
- Fix `sqlite3_malloc()` with zero argument
- Add Chicory runtime to sqlite-embedder-chicory api dependencies

### 🤖 Dependencies

- Bump Chasm to 0.9.62 

## [0.1-beta01] — 2025-03-14

- Publish to MavenCentral

## [0.1-alpha04] — 2024-06-20

### Added

- Add experimental androidx.sqlite.SQLiteDriver implementation

### Removed

- Multithreading (not working)

## [0.1-alpha03] — 2024-05-11 

### Added

- Embedders based on [Chicory] and [Chasm]
- Single-threaded build of SQLite and ICU libraries

### Changed

- Many different changes and library updates.
  - SQLite 3.45.3
  - ICU 74.2
  - Emscripten 3.1.58

[Chicory]: https://github.com/dylibso/chicory
[Chasm]: https://github.com/CharlieTap/chasm

## Version 0.1-alpha02

- Various changes. Build with localized collators support.

## Version 0.1-alpha01

- Not yet released

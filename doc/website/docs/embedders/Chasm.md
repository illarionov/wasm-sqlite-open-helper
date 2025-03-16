---
sidebar_label: 'Chasm'
sidebar_position: 1
description: 'Chasm SQLite Embedder'
---

# Chasm embedder

[Chasm] is an experimental WebAssembly runtime built on Kotlin Multiplatform.

Key Features:

- Kotlin Multiplatform solution.
- Compatible with Android API 26+ and JVM JDK 17+.
- Single threaded only.

Installation:

```kotlin
dependencies {
    testImplementation("at.released.wasm-sqlite-driver:sqlite-embedder-chasm:0.1-beta01")

    // Sqlite WebAssembly binary
    testImplementation("at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-349:0.7")
}
```

Usage:

```
val factory = WasmSqliteOpenHelperFactory(ChasmSqliteEmbedder) {
    // Configuration of the Chasm embedder
    embedder {
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcu349
    }
}
```

Just like with the [Chicory embedder](Chicory.md), when used with a room, a single-threaded coroutine dispatcher should be used.

```kotlin
newSingleThreadContext("RoomDatabase").use { singleThreadContext ->
    val db = Room.databaseBuilder()
        .setDriver(wasmSqliteDriver)
        .setQueryCoroutineContext(singleThreadContext)
        .build()
    // …    
}
```

[Chasm]: https://github.com/CharlieTap/chasm

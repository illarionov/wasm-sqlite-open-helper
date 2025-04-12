---
sidebar_label: 'Chicory'
sidebar_position: 2
description: 'Chicory SQLite Embedder'
---

# Chicory embedder

[Chicory] is a zero-dependency, pure Java runtime for WebAssembly.

The embedder implemented on it allows us to execute SQLite WebAssembly with minimal dependencies.

Key Features:

- Compatible with Android API 28+ and JVM JDK 17+.
- Simple JVM-only runtime with minimal dependencies.
- Single-threaded only.

When using this embedder, you must use SQLite compiled without multithreading support.

Installation:

```kotlin
dependencies {
    implementation("com.dylibso.chicory:runtime:1.2.1")
    implementation("at.released.wasm-sqlite-driver:sqlite-embedder-chicory:0.1-beta03")
}
```

Special versions of SQLite have been prepared to use with Chicory, compiled ahead of time into .class files.

Add dependencies:

```kotlin
  implementation("at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-aot-349:0.7")
```

Usage:

```kotlin
val wasmSqliteDriver = WasmSQLiteDriver(ChicorySqliteEmbedder) {
    embedder {
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcuAot349
        machineFactory = ::SqliteAndroidWasmEmscriptenIcuAot349Machine
    }
}
```

Other versions of SQLite can also be used:

```kotlin
dependencies {
    implementation("at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-349:0.7")
}
```

```kotlin
val factory = WasmSqliteOpenHelperFactory(ChicorySqliteEmbedder) {
    embedder {
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcu349
    }
}
```

The building of the WebAssembly SQLite libraries has been moved to the [wasm-sqlite-driver-binary] repository.
Please check it out to see the available binaries.

Make sure you are using a single threaded coroutine dispatcher when using with Room:

```kotlin
newSingleThreadContext("RoomDatabase").use { singleThreadContext ->
    val db = Room.databaseBuilder()
        .setDriver(wasmSqliteDriver)
        .setQueryCoroutineContext(singleThreadContext)
        .build()
    // …    
}
```

[Chicory]: https://github.com/dylibso/chicory
[wasm-sqlite-driver-binary]: https://github.com/illarionov/wasm-sqlite-driver-binary

---
sidebar_label: 'WasmSQLiteDriver'
sidebar_position: 2
---

# WasmSQLiteDriver

*WasmSQLiteDriver* is an implementation of the [androidx.sqlite.SQLiteDriver] interface, enabling 
SQLite usage in both JVM and Android projects.

## Installation

Add sqlite-driver dependency:

```kotlin
dependencies {
    implementation("at.released.wasm-sqlite-driver:sqlite-driver:0.1-beta02")
}
```

Add a runtime environment for executing SQLite Wasm code. Currently, the primary runtime is based on the [Chicory] library.

```kotlin
implementation("at.released.wasm-sqlite-driver:sqlite-embedder-chicory:0.1-beta02")
implementation("com.dylibso.chicory:runtime:1.1.1")
```

Add the SQLite WebAssembly binary dependency. Although the binary file for SQLite is replaceable,
the main version is currently SQLite AOT compiled into an .class for use with Chicory:

```kotlin
implementation("at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-aot-349:0.7")
```

## Usage

Create SQLite Driver using `WasmSQLiteDriver`:

```kotlin
import at.released.wasm.sqlite.binary.aot.SqliteAndroidWasmEmscriptenIcuAot349
import at.released.wasm.sqlite.binary.aot.SqliteAndroidWasmEmscriptenIcuAot349Machine
import at.released.wasm.sqlite.driver.WasmSQLiteDriver
import at.released.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedder

val wasmSqliteDriver = WasmSQLiteDriver(ChicorySqliteEmbedder) {
    embedder {
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcuAot349
        machineFactory = ::SqliteAndroidWasmEmscriptenIcuAot349Machine
    }
}
```

This driver can be used either standalone or with Android Room (2.7+).
Here’s an example of how it might be used in tests:

```kotlin
import android.content.ContextWrapper

val mockContext = object : ContextWrapper(null) {
    override fun getDatabasePath(name: String?): File = File(name!!)
}

val db = Room.databaseBuilder(
    name = dbFile.absolutePath,
    factory = ::UserDatabase_Impl,
    context = mockContext,
)
    .setJournalMode(WRITE_AHEAD_LOGGING)
    .setDriver(wasmSqliteDriver)
    .setQueryCoroutineContext(testScope.backgroundScope.coroutineContext) // or newSingleThreadContext("RoomDatabase")
    .allowMainThreadQueries()
    .build()
```

All available customization options are documented on the [Customization](Customization.md) page.

### Experimental runtimes and binaries

In addition to the Chicory runtime, experimental runtimes based on GraalVM and Chasm have also been implemented,
though they should be used for experimentation only.

```kotlin
// Runtime environment for executing SQLite Wasm code based on GraalVM (Sqlite Embedder)
implementation("at.released.wasm-sqlite-driver:sqlite-embedder-graalvm:0.1-beta02")

// implementation of the runtime environment based on the Chasm library. 
implementation("at.released.wasm-sqlite-driver:sqlite-embedder-chasm:0.1-beta02")
```

You can explore all available embedders on the [Embedders](embedders/index.md) page.

Alternative SQLite builds are also supported. The building of the WebAssembly SQLite libraries has been moved to the [wasm-sqlite-driver-binary] repository.
Please check it out to see the available binaries.

[androidx.sqlite.SQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/SQLiteDriver
[wasm-sqlite-driver-binary]: https://github.com/illarionov/wasm-sqlite-driver-binary
[Chicory]: https://chicory.dev/

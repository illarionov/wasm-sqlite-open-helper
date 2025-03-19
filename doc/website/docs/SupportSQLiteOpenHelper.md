---
sidebar_label: 'SupportSQLiteOpenHelper'
sidebar_position: 3
---

# SupportSQLiteOpenHelper

The implementation of the [androidx.sqlite.SupportSQLiteOpenHelper] interface enables the use of our 
SQLite implementation in JVM and Android projects with Room 2.6.x.

## Installation

Add the sqlite-open-helper dependency:

```kotlin
implementation("at.released.wasm-sqlite-driver:sqlite-open-helper:0.1-beta02")
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

Create Factory using `WasmSqliteOpenHelperFactory`:

```kotlin
import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import at.released.wasm.sqlite.binary.aot.SqliteAndroidWasmEmscriptenIcuAot349
import at.released.wasm.sqlite.binary.aot.SqliteAndroidWasmEmscriptenIcuAot349Machine
import at.released.wasm.sqlite.open.helper.WasmSqliteOpenHelperFactory
import at.released.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedder

val factory : SupportSQLiteOpenHelper.Factory = WasmSqliteOpenHelperFactory(context, ChicorySqliteEmbedder) {
    embedder {
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcuAot349
        machineFactory = ::SqliteAndroidWasmEmscriptenIcuAot349Machine
    }
}
```

The created factory can be used either standalone or with Android Room (2.6.x):

```kotlin
db = Room.databaseBuilder(mockContext, TestDatabase::class.java, "test")
    .openHelperFactory(factory)
    .build()
userDao = db.getUserDao()

// …

db.close()
```

All available customization options are documented on the [Customization](Customization.md) page. 

### Experimental runtimes and binaries

In addition to the Chicory runtime, experimental runtimes based on GraalVM and Chasm have also been implemented,
though they should be used for experimentation only.

```kotlin
// Runtime environment for executing SQLite Wasm code based on GraalVM (Sqlite Embedder)
implementation("at.released.wasm-sqlite-driver:sqlite-embedder-graalvm:0.1-beta02")

// Optional: implementation of the runtime environment based on the Chasm library. 
implementation("at.released.wasm-sqlite-driver:sqlite-embedder-chasm:0.1-beta02")
```

You can explore all available embedders on the [Embedders](embedders/index.md) page.

Alternative SQLite builds are also supported. The building of the WebAssembly SQLite libraries has been moved to the [wasm-sqlite-driver-binary] repository.
Please check it out to see the available binaries.

[androidx.sqlite.SupportSQLiteOpenHelper]: https://developer.android.com/reference/androidx/sqlite/db/SupportSQLiteOpenHelper
[Chicory]: https://chicory.dev/
[wasm-sqlite-driver-binary]: https://github.com/illarionov/wasm-sqlite-driver-binary

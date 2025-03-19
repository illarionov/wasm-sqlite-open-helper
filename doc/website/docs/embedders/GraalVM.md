---
sidebar_label: 'GraalWasm'
sidebar_position: 3
description: 'GraalWasm SQLite Embedder'
---

# GraalWasm embedder

[GraalWasm] is a WebAssembly engine implemented in GraalVM. It can interpret and compile WebAssembly programs in the
binary format, or be embedded into other programs.
We use it as our primary option for executing WebAssembly code.

Key features:

- Compatible with JVM JDK 21+.
- Under certain conditions, allows JIT compilation for improved performance.

Installation:

```kotlin
dependencies {
    testImplementation("at.released.wasm-sqlite-driver:sqlite-embedder-graalvm:0.1-beta02")

    // Sqlite WebAssembly binary compiled with multithreading enabled 
    testImplementation("at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-mt-pthread-349:0.1-beta02")
}
```

Usage:

```kotlin
val driver = WasmSQLiteDriver(GraalvmSqliteEmbedder) {
    // Configuration of the Wasm embedder (GraalVM)
    embedder {
        // Instance of the GraalVM WebAssembly engine. Single instance of the Engine can be reused to
        // speed up initialization.
        // See https://www.graalvm.org/latest/reference-manual/embed-languages/#managing-the-code-cache
        graalvmEngine = Engine.create("wasm")

        // Used Sqlite WebAssembly binary file
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcuMtPthread349
    }
}
```

The GraalVM embedder works only on JVM and is not compatible with Android (although it can sometimes be used with
Android local unit tests).

Additionally, the GraalVM embedder currently does not support multithreading. When integrating with Room,
ensure you use a coroutine dispatcher that runs all tasks on the same thread where the driver was created.
Alternatively, you can try to use the thread factory provided by the driver to run on a different thread:

```kotlin
val dispatcher = Executors.newSingleThreadScheduledExecutor(wasmSqliteDriver.runtime.managedThreadFactory)
    .asCoroutineDispatcher()

    dispatcher.use { queryContext ->
        val db = Room.databaseBuilder()
            .setDriver(wasmSqliteDriver)
            .setQueryCoroutineContext(queryContext)
            .build()
        // …
    }
```

By default, GraalVM executes code in interpreter mode, which can be slow. However, GraalVM offers runtime optimizations
to improve performance. For more details, see this link: https://www.graalvm.org/latest/reference-manual/embed-languages/#runtime-optimization-support

To run tests with runtime optimizations enabled, you can use Gradle toolchains to execute Android unit tests
on a compatible GraalVM version.

```kotlin
android {
    testOptions {
        unitTests.all {
            it.javaLauncher = javaToolchains.launcherFor {
                languageVersion = JavaLanguageVersion.of(22)
                vendor = JvmVendorSpec.GRAAL_VM
            }
        }
    }
}
```

To enable optimizations when running code on OpenJDK or other non-GraalVM runtime, you'll need to use the
`-XX:+EnableJVMCI` option and add the GraalVM compiler to the `--upgrade-module-path` classpath.
This can be tricky to set up with Gradle. Additionally, the GraalVM version used in the project requires JDK 22 or
later to run GraalVM Compiler. For an example of how to enable the GraalVM compiler in Android unit tests, take a look
at [this gist](https://gist.github.com/illarionov/9ce560f95366649876133c1634a03b88).

You can reuse a single instance of the GraalVM Engine between tests to speed up initialization. Specify
the instance using the `graalvmEngine` parameter in the `embedder` block when creating the factory.
Check this link for more information: https://www.graalvm.org/latest/reference-manual/embed-languages/#managing-the-code-cache

[GraalWasm]: https://www.graalvm.org/latest/reference-manual/wasm/

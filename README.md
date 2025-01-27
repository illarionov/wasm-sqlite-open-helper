# Wasm SQLite Open Helper

Experimental implementation of [androidx.sqlite.SQLiteDriver] and [androidx.sqlite.SupportSQLiteOpenHelper] 
based on SQLite compiled for WebAssembly.

Currently, works on JVM only.

It can be used to run small Android unit tests using a SQLite database inside the JVM on your host without using
an Android emulator or Robolectric framework.

## Requirements

- Java JVM 21+ when used in unit tests on the host

## Installation

Release and snapshot versions of the library are published to a temporary repository, since it is highly experimental.
File a bug report if you think it could be useful on Maven Central.

Add the following to your project's settings.gradle:

```kotlin
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pixnews.ru")
            mavenContent {
                includeGroup("ru.pixnews.wasm-sqlite-open-helper")
            }
        }
    }
}
```

You can also download a snapshot of the repository from the [Releases section](https://github.com/illarionov/wasm-sqlite-open-helper/releases) 

Add the dependencies:

```kotlin
dependencies {
    // Implementation of the SQLiteDriver (androidx.sqlite 2.5.0+)
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-driver:0.1-alpha04")

    // Implementation of the SupportSQLiteOpenHelper (androidx.sqlite 2.4.x)
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-open-helper:0.1-alpha04")

    // Runtime environment for executing SQLite Wasm code based on GraalVM (Sqlite Embedder)
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-embedder-graalvm:0.1-alpha04")

    // Optional: implementation of the runtime environment based on the Chicory library. 
    // It can be used to execute webassembly code instead of the GraalVM embedder
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-embedder-chicory:0.1-alpha04")

    // Optional: implementation of the runtime environment based on the Chasm library. 
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-embedder-chasm:0.1-alpha04")

    // Sqlite WebAssembly binary
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-android-wasm-emscripten-icu-348:0.4")
}
```

## Usage

### SQLiteDriver implementation

Create SQLite Driver using `WasmSQLiteDriver`:

```kotlin
import ru.pixnews.wasm.sqlite.driver.WasmSQLiteDriver
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedder

val wasmSqliteDriver = WasmSQLiteDriver(GraalvmSqliteEmbedder) {
}

```

This driver can be used either standalone or with Android Room (2.7+):

```kotlin
val mockContext = ContextWrapper(null)

val db = Room.databaseBuilder(
    name = dbFile.absolutePath,
    factory = ::UserDatabase_Impl,
    context = mockContext,
)
    .setJournalMode(WRITE_AHEAD_LOGGING)
    .setDriver(wasmSqliteDriver)
    .setQueryCoroutineContext(testScope.backgroundScope.coroutineContext)
    .allowMainThreadQueries()
    .build()
```

### SupportSQLiteOpenHelper implementation

Create Factory using `WasmSqliteOpenHelperFactory`:

```kotlin
import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedder

val factory : SupportSQLiteOpenHelper.Factory = WasmSqliteOpenHelperFactory(context, GraalvmSqliteEmbedder) {
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

## WebAssembly embedders

To run WebAssembly code on the JVM, we use an embedder implemented on the [GraalWasm](#GraalWasm-embedder) WebAssembly
Engine.
Alternatively, there are two other embedders: one based on [Chicory](#Chicory-embedder) WebAssembly Runtime and
another based on the [Chasm](#Chasm-embedder) library.

### GraalWasm embedder

[GraalWasm] is a WebAssembly engine implemented in GraalVM. It can interpret and compile WebAssembly programs in the
binary format, or be embedded into other programs.
We use it as our primary option for executing WebAssembly code.

Key features:

- Compatible with JVM JDK 21+.
- Under certain conditions, allows JIT compilation for improved performance.

Installation:

```kotlin
dependencies {
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-embedder-graalvm:0.1-alpha04")

    // Sqlite WebAssembly binary compiled with multithreading enabled 
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-android-wasm-emscripten-icu-mt-pthread-345:0.1-alpha04")
}
```

Usage:

```
val driver = WasmSQLiteDriver(GraalvmSqliteEmbedder) {
    // Configuration of the Wasm embedder (GraalVM)
    embedder {
        // Instance of the GraalVM WebAssembly engine. Single instance of the Engine can be reused to
        // speed up initialization.
        // See https://www.graalvm.org/latest/reference-manual/embed-languages/#managing-the-code-cache
        graalvmEngine = Engine.create("wasm")

        // Used Sqlite WebAssembly binary file
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcuMtPthread345
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

### Chicory embedder

[Chicory] is a zero-dependency, pure Java runtime for WebAssembly.

The embedder implemented on it allows us to execute SQLite WebAssembly with minimal dependencies.

Key Features:

- Compatible with Android API 33+ and JVM JDK 17+.
- Simple JVM-only runtime with minimal dependencies.
- Single-threaded only.

It can be used on Android API 33+.

When using this embedder, you must use SQLite compiled without multithreading support.

Installation:

```kotlin
dependencies {
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-embedder-chicory:0.1-alpha04")

	// Sqlite WebAssembly binary
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-android-wasm-emscripten-icu-348:0.4")
}
```

Usage:

```
val factory = WasmSqliteOpenHelperFactory(ChicorySqliteEmbedder) {
    // Configuration of the Chicory embedder
    embedder {
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcu346
    }
}
```

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


### Chasm embedder

[Chasm] is an experimental WebAssembly runtime built on Kotlin Multiplatform.

Key Features:

- Kotlin Multiplatform solution.
- Compatible with Android API 26+ and JVM JDK 17+.
- Single threaded only.

Installation:

```kotlin
dependencies {
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-embedder-chasm:0.1-alpha04")

    // Sqlite WebAssembly binary
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-android-wasm-emscripten-icu-348:0.4")
}
```

Usage:

```
val factory = WasmSqliteOpenHelperFactory(ChasmSqliteEmbedder) {
    // Configuration of the Chasm embedder
    embedder {
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcu346
    }
}
```

Just like with the Chicory embedder, when used with a room, a single-threaded coroutine dispatcher should be used.

## SQLite binaries

The building of the WebAssembly SQLite libraries has been moved to
[wasm-sqlite-driver-binary](https://github.com/illarionov/wasm-sqlite-driver-binary) repository.
Please check it out to see the available binaries.

### Customization

Below is a list of the main customization options:

```kotlin
val factory = WasmSqliteOpenHelperFactory(GraalvmSqliteEmbedder) {
    // Path resolver is used to resolve the database file path on the file system.
    // Not used for in-memory databases.
    pathResolver = DatabasePathResolver { name -> File(dstDir, name) }

    // Logger used to log debug messages from SupportSQLiteOpenHelper.
    // By default, messages are not logged
    logger = Logger

    // Parameters used when opening a database
    openParams {
        // Flags to control database access mode.
        // See https://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.OpenParams.Builder#setOpenFlags(int)
        // Combination of OPEN_READONLY, OPEN_READWRITE, OPEN_CREATE, NO_LOCALIZED_COLLATORS, ENABLE_WRITE_AHEAD_LOGGING
        openFlags = EMPTY

        // Default locale to open the database. Default: en_US
        locale = Locale("ru_RU")

        // Configures lookaside memory allocator https://sqlite.org/malloc.html#lookaside
        // If not set, SQLite defaults will be used
        setLookasideConfig(
            slotSize = 1200,
            slotCount = 100,
        )

        // Sets the journal mode for databases associated with the current database connection
        // The journalMode for an in-memory database is either MEMORY or OFF.
        // By default, TRUNCATE mode will be used if WAL is not enabled. 
        journalMode = WAL

        // Sets the filesystem sync mode
        // The default sync mode will be NORMAL if WAL is enabled, and FULL otherwise.
        syncMode = NORMAL
    }

    // Configuration of the Wasm embedder (GraalVM)
    embedder {
        // Instance of the GraalVM WebAssembly engine. Single instance of the Engine can be reused to
        // speed up initialization.
        // See https://www.graalvm.org/latest/reference-manual/embed-languages/#managing-the-code-cache
        graalvmEngine = Engine.create("wasm")

        // Sets used Sqlite WebAssembly binary file
        sqlite3Binary = SqliteAndroidWasmEmscriptenIcuMtPthread346
    }

    // Sets the debugging options
    debug {
        // Controls the printing of SQL statements as they are executed.
        set(SqliteStatementLogger)

        // Controls the printing of wall-clock time taken to execute SQL statements as they are executed.
        set(SqliteStatementProfileLogger)

        //  Enables database performance testing instrumentation.
        set(SqliteSlowQueryLogger)
    }
}
```

The intended purpose of the library is to be used in unit tests on the host. An example of such a test:

```kotlin
class DatabaseTest {
    // Android Сontext is not used in the wrapper, but is defined in the public interface of
    // the SupportSQLiteOpenHelper. So we use a mock context
    val mockContext = ContextWrapper(null)

    @TempDir
    lateinit var tempDir: File

    lateinit var db: TestDatabase
    lateinit var userDao: UserDao

    @BeforeEach
    fun createDb() {
        val openHelperFactory = WasmSqliteOpenHelperFactory(GraalvmSqliteEmbedder) {
            pathResolver = DatabasePathResolver { name -> File(tempDir, name) }

            embedder {
                graalvmEngine = Engine.create("wasm")
                sqlite3Binary = SqliteAndroidWasmEmscriptenIcuMtPthread346
            }

            debug {
                set(SqliteSlowQueryLogger)
            }
        }

        db = Room.databaseBuilder(mockContext, TestDatabase::class.java, "test")
            .openHelperFactory(openHelperFactory)
            .allowMainThreadQueries()
            .build()
        userDao = db.getUserDao()
    }

    @AfterEach
    fun closeDb() {
        db.close()
    }

    @Test
    fun dbTest() {
        val user: User = TestUtil.createUser(3).apply {
            setName("george")
        }
        userDao.insert(user)
        val byName = userDao.findUsersByName("george")
        assertThat(byName.get(0), equalTo(user))
    }
}
```

Android assets are not available in Android unit tests; however, you can use the SQLite WebAssembly binary
for the JVM target instead:

```kotlin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

dependencies {
    constraints {
        testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-android-wasm-emscripten-icu-348:*") {
            attributes {
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            }
        }
    }
}
```

## Contributing

Any type of contributions are welcome. Please see the [contribution guide](CONTRIBUTING.md).

## License

These services are licensed under Apache 2.0 License. Authors and contributors are listed in the
[Authors](AUTHORS) file.

```
Copyright 2024 wasm-sqLite-open-helper project authors and contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[Chasm]: https://github.com/CharlieTap/chasm
[Chicory]: https://github.com/dylibso/chicory
[GraalWasm]: https://www.graalvm.org/latest/reference-manual/wasm/
[androidx.sqlite.SQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/SQLiteDriver
[androidx.sqlite.SupportSQLiteOpenHelper]: https://developer.android.com/reference/androidx/sqlite/db/SupportSQLiteOpenHelper

---
sidebar_label: 'Customization'
sidebar_position: 5
---

# Customization

Below is a list of the main customization options:

```kotlin
val factory = WasmSqliteOpenHelperFactory(GraalvmSqliteEmbedder) {
    // Path resolver is used to resolve the database file path on the file system.
    // Not used for in-memory databases.
    pathResolver = DatabasePathResolver { name -> File(dstDir, name) }

    // Logger used to log debug messages from SupportSQLiteOpenHelper.
    // By default, messages are not logged
    logger = Logger`

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

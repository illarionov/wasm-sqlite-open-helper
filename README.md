# Wasm SQLite Open Helper

Experimental implementation of [androidx.sqlite.SQLiteDriver] and [androidx.sqlite.SupportSQLiteOpenHelper] 
based on SQLite compiled for WebAssembly.

It can be used to run small Android unit tests using a SQLite database inside the JVM on your host without using
an Android emulator or Robolectric framework. Although in some cases it also works on Android.

For more information, visit the project website: [wsoh.released.at](https://wsoh.released.at)

[Chasm]: https://github.com/CharlieTap/chasm
[Chicory]: https://github.com/dylibso/chicory
[GraalWasm]: https://www.graalvm.org/latest/reference-manual/wasm/
[androidx.sqlite.SQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/SQLiteDriver
[androidx.sqlite.SupportSQLiteOpenHelper]: https://developer.android.com/reference/androidx/sqlite/db/SupportSQLiteOpenHelper

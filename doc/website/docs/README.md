---
sidebar_label: 'Overview'
sidebar_position: 1
---

# Wasm SQLite Open Helper

This project provides experimental implementations of [androidx.sqlite.SQLiteDriver] and [androidx.sqlite.SupportSQLiteOpenHelper],
built on SQLite compiled for WebAssembly.  
It allows running small Android unit tests with a SQLite database directly on the JVM, without requiring an Android 
emulator or the Robolectric framework. In some cases it also works on Android.

## Requirements

- Java JVM 21+ when used in unit tests on the host
- Android API 28+ when running on Android 

## Installation

The latest release is available on [Maven Central].

```kotlin
repositories {
    mavenCentral()
}
```

Snapshot versions of the library may be published to a self-hosted public repository.

```kotlin
pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pixnews.ru")
            mavenContent {
                includeGroup("at.released.wasm-sqlite-driver")
            }
        }
    }
}
```

You can also download a snapshot of the repository from the [Releases section](https://github.com/illarionov/wasm-sqlite-open-helper/releases)

## Usage

The [androidx.sqlite.SQLiteDriver] interface provides an abstraction over the low-level SQLite APIs.
It was introduced in the `androidx.sqlite` 2.5.0 for Kotlin Multiplatform projects and is intended for building 
higher-level libraries like Room or for direct use.  
You can find the usage guide for our implementation on the [WasmSQLiteDriver](WasmSQLiteDriver.md) page.

The [androidx.sqlite.SupportSQLiteOpenHelper] interface also abstracts the SQLite API and 
was originally defined in the `androidx.sqlite` library for use in Android projects.
It mirrors the Android [SQLiteOpenHelper][android.database.sqlite.SQLiteOpenHelper] and was used 
in the Room library until version 2.7.0.  
For details on using our implementation of this interface, please refer to the [SupportSQLiteOpenHelper](SupportSQLiteOpenHelper.md) page.

## Contributing

Any type of contributions are welcome. Please see the [contribution guide](https://github.com/illarionov/wasm-sqlite-open-helper/blob/main/CONTRIBUTING.md).

## License

These services are licensed under Apache 2.0 License. Authors and contributors are listed in the
[Authors](https://github.com/illarionov/wasm-sqlite-open-helper/blob/main/AUTHORS) file.

```
Copyright 2024-2025 wasm-sqLite-open-helper project authors and contributors.

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


[Maven Central]: https://central.sonatype.com/artifact/at.released.wasm-sqlite-driver/sqlite-driver
[androidx.sqlite.SQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/SQLiteDriver
[androidx.sqlite.SupportSQLiteOpenHelper]: https://developer.android.com/reference/androidx/sqlite/db/SupportSQLiteOpenHelper
[android.database.sqlite.SQLiteOpenHelper]: https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper

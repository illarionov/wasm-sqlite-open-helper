---
sidebar_label: 'Overview'
sidebar_position: 1
---

# Wasm SQLite Open Helper

Experimental implementation of [androidx.sqlite.SQLiteDriver] and [androidx.sqlite.SupportSQLiteOpenHelper]
based on SQLite compiled for WebAssembly.

It can be used to run small Android unit tests using a SQLite database inside the JVM on your host without using
an Android emulator or Robolectric framework.

## Requirements

- Java JVM 21+ when used in unit tests on the host

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

[Maven Central]: https://central.sonatype.com/artifact/at.released.wasm-sqlite-driver/sqlite-driver
[androidx.sqlite.SQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/SQLiteDriver
[androidx.sqlite.SupportSQLiteOpenHelper]: https://developer.android.com/reference/androidx/sqlite/db/SupportSQLiteOpenHelper

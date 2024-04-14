# Wasm SQLite Open Helper

Implementation of [SupportSQLiteOpenHelper] based on SQLite compiled for WASM.

WIP

## Installation

Release and snapshot versions of the library are published to a temporary repository, since it is very experimental
and at the moment it is in an early stage of development and is planned for use in only one project.
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

Add the dependencies:

```kotlin

dependencies {
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-open-helper:0.1-alpha02")
    testImplementation("ru.pixnews.wasm-sqlite-open-helper:sqlite-embedder-graalvm:0.1-alpha02")
}
```

## Usage

The library can be used with Room in Android unit tests.

Sample:

```kotlin
class DatabaseTest {
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
            }
            debug {
                sqlLog = true
                sqlTime = true
                sqlStatements = true
                logSlowQueries = true
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


[SupportSQLiteOpenHelper]: https://developer.android.com/reference/androidx/sqlite/db/SupportSQLiteOpenHelper

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

---
sidebar_label: 'Examples'
----

# Examples

The intended purpose of the library is to be used in unit tests on the host. An example of such a test:

```kotlin
class DatabaseTest {
    // Android Сontext is used only to resolve database path so we use a mock context
    val mockContext = object : ContextWrapper(null) {
        override fun getDatabasePath(name: String?): File = File(name!!)
    }

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
        testImplementation("at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-349:*") {
            attributes {
                attribute(KotlinPlatformType.attribute, KotlinPlatformType.jvm)
            }
        }
    }
}
```

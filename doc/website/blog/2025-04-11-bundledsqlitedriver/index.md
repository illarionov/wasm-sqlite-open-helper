---
slug: bundledsqlitedriver
title: BundledSQLiteDriver, A New Look at SQLite in Android and Kotlin Multiplatform
authors: [illarionov]
---
import BundledSQLiteDriverBenchmarkReport from './BundledSQLiteDriverBenchmarkReport';

__BundledSQLiteDriver__ is a custom build of SQLite created by the Android Jetpack team to support 
Kotlin Multiplatform projects.  
In this article, we will explore its architecture, features, performance improvements, and compatibility trade-offs.
It’s especially relevant for developers looking to build cross-platform apps with unified database behavior and
access to newer SQLite features.

<!-- truncate -->

## Room and SQLite

In early 2024, Google officially announced Kotlin Multiplatform support for __Room__, their ORM-style library for
accessing local SQLite‑based storage.
Subsequently on April 9, 2025, [Room 2.7.0] — the first stable version with Multiplatform support — was released.

From the very early versions of this library, low‑level access to SQLite is not performed directly
but via the abstractions provided by the interfaces in the `androidx.sqlite:sqlite` library:

* `SupportSQLiteOpenHelper`
* `SupportSQLiteDatabase`
* `SupportSQLiteStatement`

This separation — following an API – Implementation style — allows the underlying SQLite version to be replaced as needed.
The main implementation — the `FrameworkSQLiteOpenHelper` class — resides in the `androidx.sqlite:sqlite-framework` module.

Several third-party libraries implement these APIs, offering their own SQLite variants. Notable examples include
[SQLCipher] and [requery/sqlite‑android].

Moreover, these interfaces are not exclusive to Room. For instance, the [SQLDelight] library also uses them.
The [SQLDelight AndroidSqliteDriver](https://sqldelight.github.io/sqldelight/2.0.2/2.x/drivers/android-driver/app.cash.sqldelight.driver.android/-android-sqlite-driver/index.html#-1882063159%2FConstructors%2F1288973086) adapter takes an 
`openHelper: SupportSQLiteOpenHelper` parameter, which makes it possible to use in SQLDelight, for example, requery‑android.

In general, the _SupportSQLite\*_ interfaces mirrored Android platform classes — _SQLiteOpenHelper_,
_SQLiteDatabase_, _SQLiteStatement_.
Many Android system classes are used in public definitions: _Cursor_, _ContentObserver_, 
_ContentResolver_, _android.net.Uri_, _Handler_, _Bundle_, _Context_ (for a single method only).
As a result, these interfaces turned out to be completely unsuitable for use in Kotlin Multiplatform projects.

Starting from version 2.5.0, the `androidx.sqlite` library was ported to KMP.
The original `SupportSQLite*` interfaces were preserved in the Android variant of the library for 
backward compatibility, and a new set of interfaces was introduced in the common source set:

* `SQLiteDriver` — entry point for opening a database and establishing a connection
* `SQLiteConnection` — an object for interacting with the database and a factory for creating SQLiteStatement.
* `SQLiteStatement` — an abstraction over SQLite's prepared statement used for executing queries.

This new version more closely resembles the SQLite C API. In short, it can be described as follows:

```kotlin
package androidx.sqlite

interface SQLiteDriver {
  fun open(filename: String): SQLiteConnection
}

interface SQLiteConnection : AutoCloseable {
  fun prepare(sql: String): SQLiteStatement
}

interface SQLiteStatement : AutoCloseable {
  fun bindT(index: Int, value: T)
  fun getT(index: Int): T
  fun step(): Boolean
}
```

It has become much simpler, shorter, and more universal than the previous version.
Because these are plain interfaces, original instances of the drivers can be decorated to implement additional features
such as logging, profiling, query analytics, encryption, and more.

Three implementations of new APIs were also presented:

* __[AndroidSQLiteDriver]__ — based on Android SDK SQLite APIs (Android only).
* __[NativeSQLiteDriver]__ — a variant for iOS, Mac, and Linux. In this version, SQLite is linked dynamically, and the end user’s system library `libsqlite` is used in the final application.
* __[BundledSQLiteDriver]__ — a custom build of SQLite that is packaged with the application. This is the recommended
variant, suitable for Android, iOS, Mac, Linux, and JVM.

Although `androidx.sqlite` is positioned as a foundation for more complex frameworks, it can also be used as a standalone library.
Now that it's a Kotlin Multiplatform project, usage is no longer confined to Android.
For example, we can use this library on the JVM host instead of SQLite JDBC or even within Gradle plugins to prepare 
a database on the host that is packaged into Android assets for use on the device with the same driver.

## BundledSQLiteDriver

Let's look at the features of the __BundledSQLiteDriver__ driver when used on Android. Here are its main advantages.

1. __A Modern version of SQLite__  
 Unlike AndroidSQLiteDriver, which uses the device's system SQLite (often outdated), __BundledSQLiteDriver__ includes a modern version.
2. __Predictable Behavior Across Devices__  
Since it uses its own packaged SQLite, you can be sure of the library version, available features, extensions, and query planner behavior on different devices.
3. __Improved Performance__  
Performance is generally better, not only because of newer SQLite version. Android’s system SQLite uses classes
originally designed for interprocess communication. Data transfer between native memory and the JVM is handled
via a fixed‑size buffer (typically 2 MB), represented on the Java side by the class
[CursorWindow] and on the native side by the [C++ class CursorWindow][cppCursorWindow]. This buffer previously
caused many issues for developers but is no longer needed in the bundled version.

Among the drawbacks, one can note an increase in the APK size by approximately 1 MB due to the bundled SQLite library
and potentially higher memory consumption.
Additionally, it cannot be used with the [Privacy Sandbox SDK Runtime] due to the use of JNI code in its implementation.

### Unique Features of AndroidSQLiteDriver

The compilation configuration of __BundledSQLiteDriver__ differs from that used in Android’s SQLite.
Let's take a look at the features that may no longer work when switching from the default AndroidSQLiteDriver.

Tested Library Versions:
* `androidx.sqlite` version: 2.5.0 (SQLite 3.46.0)
* Android device: API 33 (SQLite 3.32.2)

#### 1) Localization

Those who migrated from the Android version likely noticed that SQLite in BundledSQLiteDriver is built 
without the [ICU Extension]; as a result, many locale‑related functions do not work. In particular:

* __Case‑insensitive `LIKE` does not work:__  
    ```sql title="SQLite case-insensitive LIKE"
    CREATE TABLE Customers(id INTEGER PRIMARY KEY, name TEXT, city TEST);
    INSERT INTO Customers(name,city) VALUES ("Пользователь", "Город");

    SELECT name FROM Customers WHERE name LIKE 'пол%';

    AndroidSqliteDriver: [{name=Пользователь}]
    BundledSqliteDriver: []
    ```
* __The functions `upper()` and `lower()` do not work:__  
    ```sql title="SQLite upper() / lower()"
    SELECT upper('пользователь')
    SELECT lower('ИЗДӨӨ')
    SELECT lower('ISPANAK', 'tr_tr')

    AndroidSqliteDriver: ПОЛЬЗОВАТЕЛЬ издөө ıspanak
    BundledSqliteDriver: пользователь ИЗДӨӨ `android.database.SQLException: 
      wrong number of arguments to function lower()`
    ```
* __`COLLATE` does not work for regional sort rules; the function `icu_load_collation()` is unavailable:__
    ```sql title="SQLite collate()"
    SELECT icu_load_collation('ru_RU', 'russian')
    CREATE TABLE Customers(name TEXT COLLATE russian)
    INSERT INTO Customers(name) VALUES ('Б'), ('а')

    SELECT name FROM Customers ORDER BY name

    AndroidSqliteDriver: a, Б
    BundledSqliteDriver: `no such function: icu_load_collation`
    ```
* __The `REGEXP` operator is unavailable__
    ```sql title="SQLite ICU REGEXP"
    CREATE TABLE Customers(name TEXT)
    INSERT INTO Customers(name) VALUES ('Пользователь 😎')

    SELECT name FROM Customers WHERE name REGEXP '.+\p{Emoji}+'

    AndroidSqliteDriver: {name=Пользователь 😎}
    BundledSqliteDriver: `no such function: REGEXP`
    ```
* __`PRAGMA case_sensitive_like` is unavailable__

If the application has a limited localization requirements, a workaround can be to convert all texts to the same 
case before storing. Alternatively, you may consider using SQLite full-ext search (FTS) with the `unicode64` tokenizer.

#### 2) Android‑Specific Extensions

__BundledSQLiteDriver__ omits all Android-specific SQLite extensions:

* Only standard SQLite `COLLATE` options are available: `BINARY`, `RTRIM`, `NOCASE`.  
Android-specific ones — `LOCALIZED`, `UNICODE`, and (undocumented) `PHONEBOOK` — are not.
* All undocumented Android functions are unavailable: `PHONE_NUMBERS_EQUAL()`,`_PHONE_NUMBER_STRIPPED_REVERSED()`, `_DELETE_FILE()` (in case anyone was even aware of their existence).

#### 3) Missing options

In the current version of __BundledSQLiteDriver__, the options `SQLITE_ENABLE_BYTECODE_VTAB`
and `SQLITE_ENABLE_DBSTAT_VTAB` are not enabled.

These options were added to Android only very recently and are available only on Android API 36 (or slightly earlier).
The first option adds the functions `bytecode()` and `tables_used()`, which are intended to dump the
byte-code of the SQL query and list the tables used:

```sql title="SQLite bytecode() and tables_used()"
CREATE TABLE Customers(id INTEGER PRIMARY KEY, name TEXT)
SELECT * FROM bytecode('SELECT * FROM Customers')
SELECT * FROM tables_used('SELECT * FROM Customers')

AndroidSqliteDriver: bytecode: [
  {addr=0, opcode=Init, p1=0, p2=8, p3=0, p4=null, p5=0, 
  comment=null, subprog=null, nexec=0, ncycle=0},…
AndroidSqliteDriver: tables_used: [
  {type=table, schema=main, name=Customers, wr=0, subprog=null}
]
```
The second option adds the `dbstat` virtual table, which returns information about how much disk space is used 
by the database.

```sql title="SQLite dbstat"
SELECT * FROM dbstat

AndroidSqliteDriver: [
   {name=sqlite_schema, path=/, pageno=1, pagetype=leaf, ncell=2, payload=174, unused=3806, mx_payload=87, pgoffset=0, pgsize=4096},
   {name=android_metadata, path=/, pageno=2, pagetype=leaf, ncell=1, payload=7, unused=4077, mx_payload=7, pgoffset=4096, pgsize=4096},
   {name=Customers, path=/, pageno=3, pagetype=leaf, ncell=0, payload=0, unused=4088, mx_payload=0, pgoffset=8192, pgsize=4096}
]
```

#### 4) Deprecated Behavior

In __BundledSQLiteDriver__, many options that supported deprecated behavior have been disabled:

* PRAGMAs removed: *count_changes*, *data_store_directory*, *default_cache_size*,
*empty_result_callbacks*, *full_column_names*, *short_column_names*, *temp_store_directory*.
* Shared cache has been removed.
* `SQLITE_ALLOW_ROWID_IN_VIEW` option is disabled.

### Unique Features of BundledSQLiteDriver

Let’s now examine the features available with __BundledSQLiteDriver__ that are not available in __AndroidSQLiteDriver__
(SQLite 3.32.2).

#### 1. FTS5

In __BundledSQLiteDriver__, the new version of the full‑text search extension FTS5 is available.
FTS3 and FTS4 are still included, with FTS3 additionally enhanced with support for parentheses and the AND/NOT operators.

```sql title="SQLite FTS5"
CREATE TABLE Customers(id INTEGER PRIMARY KEY, name TEXT, city TEXT)
CREATE VIRTUAL TABLE Customers_idx USING fts5(name, content='Customers',
 content_rowid='id')

INSERT INTO Customers(name,city) VALUES ('Пользователь', 'Город')
INSERT INTO Customers_idx(Customers_idx) values('rebuild')

SELECT Customers.*
  FROM Customers_idx INNER JOIN Customers ON Customers_idx.rowid=Customers.id
  WHERE Customers_idx.name MATCH '"поль" *'
```

Check https://www.sqlite.org/fts5.html for details.

#### 2. JSON

A new set of functions and operators for working with data in JSON format has been introduced. 
It supports storing data both as text and in an internal binary format (JSONB). For example:

```sql title="SQLite JSON"
CREATE TABLE Customers(id INTEGER PRIMARY KEY, data BLOB)
INSERT INTO Customers(data) VALUES (jsonb('{"city": "City", "name": "User"}'))
SELECT id,json(data) FROM Customers WHERE data ->> '$.city' = 'User'

BundledSqliteDriver: [{id=1, json(data)={"city":"City","name":"User"}}]
```

For details, see https://www.sqlite.org/json1.html. 

#### 3. R\*-Tree Index

This algorithm is designed for spatial data indexing. The index can be used to optimize 
interval/range/coordinate search queries or for geospatial queries.

```sql title="SQLite R-Tree index"
CREATE TABLE Products(id INTEGER PRIMARY KEY, name TEXT NOT NULL)
CREATE VIRTUAL TABLE PriceRanges USING rtree(id, minPrice, maxPrice)
INSERT INTO Products (id, name) VALUES(1, 'Thermosiphon')
INSERT INTO PriceRanges VALUES(1, 115, 380)

SELECT Products.*
  FROM Products,PriceRanges ON Products.id=PriceRanges.id
  WHERE maxPrice>=300 AND minPrice <= 300


BundledSqliteDriver: {id=1, name=Thermosiphon}
```

Documentation: https://www.sqlite.org/rtree.html

#### 4. Support for RIGHT and FULL OUTER JOIN

The SQLite version used in the driver supports `RIGHT` and `FULL OUTER JOIN` clauses in `SELECT` queries.

#### 5. Support for Dropping Columns

Queries of the form `ALTER TABLE DROP COLUMN` are supported, allowing the deletion of columns that are not 
referenced by other parts of the database schema. Columns that are indexed or have foreign key constraints, 
for example, cannot be dropped.

#### 6. Enhanced UPSERT and the RETURNING Clause

In `UPSERT`, it is now possible to specify multiple `ON CONFLICT` clauses.
The `RETURNING` clause on `DELETE`, `INSERT`, and `UPDATE` queries allows you to retrieve automatically 
filled values (for example, a generated ID).

```sql title="SQLite RETURNING"
CREATE TABLE Customers(id INTEGER PRIMARY KEY, uuid TEXT UNIQUE, name TEXT)

INSERT INTO Customers(uuid,name) VALUES ('123','Customer') 
	ON CONFLICT(uuid) DO UPDATE SET name=excluded.name RETURNING id
1

INSERT INTO Customers(uuid,name) VALUES ('123','Customer') 
	ON CONFLICT(uuid) DO UPDATE SET name=excluded.name RETURNING id
1
```
For more details, see documentation at https://www.sqlite.org/lang_returning.html

In the Room library at this time, these capabilities are not used: the `@Upsert` is implemented with a pair of 
queries (`INSERT` + `UPDATE`). Additional queries such as `SELECT changes()` and `SELECT last_insert_rowid()`
are executed to retrieve the IDs of inserted records.

#### 7. Support for "UPDATE FROM" queries

A subquery in the form `UPDATE FROM` allows you to modify records in one table using data from another table.

#### 8. CTE Temporary Table Type Specification

In Common Table Expressions (queries with `WITH`), the ability to specify `AS MATERIALIZED` or `AS NOT MATERIALIZED`
has been added. These constructs allow you to define the type of temporary view created for a table in the WITH clause.

More on CTEs can be found in SQLite’s WITH documentation https://www.sqlite.org/lang_with.html

#### 9.SQLITE_ENABLE_STAT4 enabled

This parameter activates additional logic in the `ANALYZE` command, which accumulates statistics on the distribution
of index keys to improve the query planner’s index selection.  
The `ANALYZE` command is executed automatically once the `PRAGMA optimize` command is run periodically.

See https://www.sqlite.org/lang_analyze.html

#### 10. Enhanced Date and Time Functions

* Added the function `timediff()`.
* New specifiers in `strftime()`: `%e %F %I %k %l %p %P %R %T %u %G %g %U, %V`.
* `ceiling` and `floor` modifiers when calculating dates.
* The ability to specify a time difference in the format `±YYYY‑MM‑DD HH:MM:SS.SSS`.
* The `subsec` modifier for increased precision (e.g., `SELECT unixepoch('subsec')`).
* The function `unixepoch()` with `auto` and `julianday` modifiers.

```sql title="SQLite time functions"
SELECT timediff('2025-03-21','2025-01-01')
SELECT strftime('%F %k:%l', 1743290838, 'unixepoch', 'floor')

+0000-02-20 00:00:00.000
2025-03-29 23:11
```

For further details, refer to SQLite’s Date/Time Functions documentation: https://www.sqlite.org/lang_datefunc.html#uepch

#### 11. Additional Functions and Operators

* Functions `concat()` and `concat_ws(SEP, …)`.
* `string_agg()` – an alias for `group_concat()`.
* Aggregate functions now support an `ORDER_BY` clause; this is particularly useful for `string_agg()` and `json_group_array()`.
* Operators `IS DISTINCT FROM` and `IS NOT DISTINCT FROM` — aliases for `IS NOT` and `IS`, respectively.
* `printf()` – a synonym for `format()`.
* `unhex() `– returns a BLOB from a hexadecimal string.
* `octet_length()` — returns the number of bytes required to store the text representation of a number in the current encoding.

#### 12. New "PRAGMA table_list" Command

This command returns information about tables and views.

```sql title="SQLite table_list"
PRAGMA table_list

[
 {schema=main, name=sqlite_schema, type=table, ncol=5, wr=0, strict=0},
 {schema=temp, name=sqlite_temp_schema, type=table, ncol=5, wr=0, strict=0}
]
```

#### 13. "STRICT" Option in "CREATE TABLE"

Using `CREATE TABLE … STRICT` enables a strict schema definition and data insertion syntax.

Details can be found at https://www.sqlite.org/stricttables.html

#### 14. Changed Default Values

The default synchronization mode (`PRAGMA synchronous`) is set to `NORMAL` (1) instead of `FULL` (2).

## Benchmarks

I benchmarked the performance of __AndroidSQLiteDriver__ and __BundledSQLiteDriver__ on an Android device.

For testing, I used [Androidx Microbenchmark], a library that helps create a reproducible testing environment
on Android device.
It locks CPU frequencies, sets process priorities, enforces AOT compilation using `CompilationMode.FULL`,
and can pause execution if the device overheats.
While the absolute values obtained from Microbenchmark aren't particularly meaningful on their own, they serve
well for comparing different implementations of the same interface.

Before the main tests, the library runs a number of warm-up iterations — the default number of iterations was 
thoughtfully chosen by Android specialists.
In my case, around 30 warm-up runs were executed (until results stabilized) followed by 50 main runs.
Given these numbers, each test should not take long (I configured each test to take approximately 5–10 seconds).

The test database was based on the [rawg-games-dataset] in CSV format (approximately 600 MB containing 881,000 records)
and was split into several SQL tables. For each driver, three tests were performed:

* `create_database`  
  This test creates a database and fills it using batch INSERT queries, reading data from a CSV file located in the Android assets.
* `select_with_paging`  
   For a pre‑prepared database, many INSERT queries are executed. Each query has a small filter in the `WHERE`
   clause and uses paging with `OFFSET n LIMIT m` returning sets of records spanning several columns.
* `huge_select`  
  A single complex query is executed on the database, taking a significant amount of time to complete as it 
  includes complex groupings and filters. This test returns few rows.

For tests 2 and 3, the database is prepared on the host using the same code as in test 1. All tests are executed on Android 13.

Results:

<BundledSQLiteDriverBenchmarkReport data={ require('./nativetests.json') } theme={ document.documentElement.getAttribute('data-theme') } ></BundledSQLiteDriverBenchmarkReport>

<details>

<summary>Table</summary>

| Test               | AndroidSQLiteDriver, ms | BundledSQLiteDriver, ms |
|--------------------|------------------------:|------------------------:|
| create_database    |                4321.030 |                3197.938 |
| select_with_paging |                4347.375 |                3466.894 |
| huge_select        |                3688.130 |                2829.959 |

Software Versions Used:

* Androidx SQLite: 2.5.0 (SQLite 3.46.0)
* Androidx Benchmark: 1.4.0-alpha11
* SQLite on the device: 3.32.2

</details>

All tests run __20–26%__ faster with __BundledSQLiteDriver__ compared to __AndroidSQLiteDriver__.

The repository with the tests and examples is available at https://github.com/illarionov/sqlite-driver-benchmark.

In that repository, two additional implementations of SQLiteDriver are also tested with WebAssembly-compiled SQLite:
one with AOT‑compiled SQLite in .class format and another running in WebAssembly interpreters for the JVM.
You can check out the full results on the [SQLiteDriver benchmarks](benchmark) blog post.

## Conclusions

__BundledSQLiteDriver__ offers advantages for use both in Kotlin Multiplatform projects and in pure Android
applications. It enables new features and ensures predictable behavior across all devices.
However, it may not be suitable if localization or application size are critical considerations.

[Room 2.7.0]: https://developer.android.com/jetpack/androidx/releases/room#2.7.0
[SQLCipher]: https://www.zetetic.net/sqlcipher/
[requery/sqlite‑android]: https://github.com/requery/sqlite-android
[SQLDelight]: https://github.com/sqldelight/sqldelight
[AndroidSQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/driver/AndroidSQLiteDriver
[NativeSQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/driver/NativeSQLiteDriver
[BundledSQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/driver/bundled/BundledSQLiteDriver
[CursorWindow]: https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/java/android/database/CursorWindow.java;drc=6266b5b1795fbf4a986dd01485f77120fee932a1
[cppCursorWindow]: https://cs.android.com/android/platform/superproject/main/+/main:frameworks/base/core/jni/android_database_CursorWindow.cpp;drc=31eb3c89a94d77eae9503bd635a4f056db3e3a31 
[Privacy Sandbox SDK Runtime]: https://privacysandbox.google.com/private-advertising/sdk-runtime/architecture
[ICU Extension]: https://sqlite.org/src/dir/ext/icu
[Androidx Microbenchmark]: https://developer.android.com/topic/performance/benchmarking/microbenchmark-overview
[rawg-games-dataset]: https://huggingface.co/datasets/atalaydenknalbant/rawg-games-dataset

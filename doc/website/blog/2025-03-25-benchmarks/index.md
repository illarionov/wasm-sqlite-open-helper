---
slug: benchmark
title: SQLiteDriver benchmarks
authors: [illarionov]
---

import SQLiteDriverBenchmarkReport from './SQLiteDriverBenchmarkReport';

In this post, I want to share the results of my performance tests on several implementations
of the [SQLiteDriver][androidx.sqlite.SQLiteDriver]:

* __Native SQLite drivers__: *AndroidSQLiteDriver*, *BundledSQLiteDriver*
* __WebAssembly SQLite Ahead-of-time (AOT) compiled__ and running on Chicory Embedder
* __WebAssembly SQLite__ running on WebAssembly interpreters: Chicory and Chasm

[androidx.sqlite.SQLiteDriver]: https://developer.android.com/reference/kotlin/androidx/sqlite/SQLiteDriver

<!-- truncate -->

For the tests, the [rawg-games-dataset] was used as the database splitted into several SQLite tables.

The drivers under test were categorized into three groups:

For each tested SQLiteDriver configuration, three tests were performed:

##### create_database 

This test creates a new database using a predefined schema with multiple tables.
It then inserts data from the [rawg-games-dataset] in batches via INSERT queries, reading the dataset 
from a CSV file located in Android Assets.

##### select_with_paging

In this test, a series of *SELECT* queries are executed on a pre-populated database.  
These queries include paging using the *"OFFSET XXX LIMIT XXX"* clause, along with minor filtering in the *WHERE* clause.
Each query returns one row with several string columns.

##### huge_select

This test runs one complex query on the pre-populated database.  
The query involves grouping and complex filters, takes a considerable amount of time to execute.
It returns a few rows.

All tests are executed on a device running Android 13.

The repository for the project is available here: https://github.com/illarionov/sqlite-driver-benchmark

##### Versions

The following software versions are used in testing:

* Androidx SQLite: 2.5.0-rc02
* Androidx Benchmark: 1.4.0-alpha09
* Chicory: 1.1.1
* Chasm: 0.9.63
* Wasm SQLite Open Helper: 0.1-beta02
* WSOH-binary: 0.7

SQLite versions:
* AndroidSQLiteDriver (SQLite on Android device: 3.32.2)
* BundledSQLiteDriver: 3.46.0
* WebAssembly binary: 3.49.1 (without multithreading support)

### Native SQLiteDrivers

In the first group, we evaluated the performance of the native implementations of SQLiteDriver 
provided by the [androidx.sqlite] package:

* *[AndroidSQLiteDriver]* (SQLite 3.32.2 on a tested device)
* *[BundledSQLiteDriver]* (SQLite 3.46.0)

Results:

<SQLiteDriverBenchmarkReport data={ require('./native/nativetests.json') } ></SQLiteDriverBenchmarkReport>

(Graphs indicate test execution time — lower values are better.)

<details>

<summary>Table</summary>

| Test               | AndroidSQLiteDriver, ms | BundledSQLiteDriver, ms |
|--------------------|------------------------:|------------------------:|
| create_database    |                4358.786 |                3139.724 |
| select_with_paging |                4297.671 |                3479.016 |
| huge_select        |                3576.628 |                2765.599 |

Raw report: [at.released.sqlitedriverbenchmark.test-benchmarkData.json](./native/at.released.sqlitedriverbenchmark.test-benchmarkData.json)

</details>

All tests ran 20–28% faster when using *BundledSQLiteDriver* compared to *AndroidSQLiteDriver*.

### WebAssembly AOT-compiled SQLiteDriver

This group tests SQLite compiled to WebAssembly and then AOT-compiled into JVM bytecode (.class).
[Chicory Runtime](/embedders/Chicory) is used to run on an Android device.

Two Chicory configurations were tested: one using *ByteBufferMemory* and the other using 
*ByteArrayMemory* (see [Chicory Memory]). 

Drivers on the Graph:

* __Android__ —  AndroidSQLiteDriver (for reference)
* __ChicoryAot__ — Chicory AOT driver with *ByteBufferMemory*
* __ChicoryAotBA__ — Chicory AOT driver with *ByteArrayMemory*

Results:

<SQLiteDriverBenchmarkReport data={ require('./chicoryaot/chicoryaottests.json') } ></SQLiteDriverBenchmarkReport>

(Graphs indicate execution time — lower values are better.)

<details>

<summary>Table results</summary>

| Test               |   Android, ms | ChicoryAot, ms |    ChicoryAotBA, ms |
|--------------------|---------------:|---------------:|--------------------:|
| create_database    |           1348 |           6317 |               10400 |
| select_with_paging |             24 |            995 |                2004 |
| huge_select        |             12 |            735 |                1531 |

Raw report: [at.released.sqlitedriverbenchmark.test-benchmarkData.json](./chicoryaot/at.released.sqlitedriverbenchmark.test-benchmarkData.json)

</details>

For the database creation test, the *ChicoryAot* driver was 4.4 times slower than the native implementation.

In the other *SELECT* tests, the ChicoryAot implementation was more than 40 times slower.

The *ByteBufferMemory* configuration was 1.5–2 times faster than the *ByteArrayMemory* version.

During execution, the log contained warnings such as:

> Method exceeds compiler instruction limit: 43858 in int at.released.wasm.sqlite.binary.aot.SqliteWasmEmscriptenAot349Machine.func_778(int, com.dylibso.chicory.runtime.Memory, com.dylibso.chicory.runtime.Instance)

This may indicate that the lack of optimizations for large methods leads to extremely poor performance.

### WebAssembly Interpreters

This group tests the execution of SQLite compiled to WebAssembly running on JVM-based interpreters.

For Chasm, configurations with and without bytecode fusion were tested.

Drivers on the Graph:

* __Android__ —  AndroidSQLiteDriver (for reference)
* __ChicoryAot__ — Chicory AOT driver with *ByteBufferMemory* (for reference)
* __Chasm__ — Chasm interpreter with bytecode fusion enabled
* __ChasmNoFusion__ — Chasm interpreter with bytecode fusion disabled
* __Chicory__ — Chicory Interpreter with *ByteBufferMemory*
* __ChicoryBA__ — Chicory interpreter with *ByteArrayMemory*

Results:

<SQLiteDriverBenchmarkReport data={ require('./interpreters/interpretertests.json') } ></SQLiteDriverBenchmarkReport>

(Graphs indicate execution time—lower values are better.)

<details>

<summary>Table results</summary>

| Test               | Android | ChicoryAot | Chicory | ChicoryBA |  Chasm | ChasmNoFusion |
|--------------------|--------:|-----------:|--------:|----------:|-------:|--------------:| 
| create_database    |     250 |       1210 |   19830 |     21044 |  13595 |         20217 |
| select_with_paging |      15 |        883 |   12958 |     17388 |   5787 |          5229 |
| huge_select        |      12 |       1063 |   13258 |     14439 |   7787 |          7235 |

Raw report: [at.released.sqlitedriverbenchmark.test-benchmarkData.json](./interpreters/at.released.sqlitedriverbenchmark.test-benchmarkData.json)

</details>

As expected, running SQLite in WebAssembly interpreters resulted in a significant performance drop — it is at least 50 
times slower than native implementation (AndroidSQLiteDriver).

On average, Chasm managed the tests 30–50% faster than Chicory in interpreter mode.

Chicory in AOT mode was about 7 times faster than Chasm.

The performance difference between Chicory interpreter configurations using *ByteArrayMemory* versus 
 *ByteBufferMemory* was less significant, with ByteArrayMemory being 6% to 35% slower.

In Chasm, enabling bytecode fusion improved performance by 1.5x in database creation tests, but it was 8% slower 
in other tests, making its overall impact unclear.

### Conclusions

At this stage, WebAssembly runtimes for the JVM are not yet practical for running SQLite on Android.

[androidx.sqlite]: https://developer.android.com/jetpack/androidx/releases/sqlite
[rawg-games-dataset]: https://huggingface.co/datasets/atalaydenknalbant/rawg-games-dataset
[AndroidSQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/driver/AndroidSQLiteDriver
[BundledSQLiteDriver]: https://developer.android.com/reference/androidx/sqlite/driver/bundled/BundledSQLiteDriver
[Chicory Memory]: https://chicory.dev/docs/advanced/memory

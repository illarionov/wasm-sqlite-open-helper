/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.test.base

import android.content.ContextWrapper
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.test.utils.KermitLogger

abstract class AbstractOpenHelperFactoryTest<E : SqliteEmbedderConfig>(
    private val factoryCreator: TestOpenHelperFactoryCreator,
    dbLoggerSeverity: Severity = Info,
) {
    protected open val logger = KermitLogger(this::class.java.simpleName)
    protected open val dbLogger = KermitLogger(tag = "WSOH", minSeverity = dbLoggerSeverity)
    abstract val tempDir: String

    open fun createWasmSQLiteOpenHelperFactory(
        sqlite3Binary: WasmSqliteConfiguration = factoryCreator.defaultSqliteBinary,
    ): SupportSQLiteOpenHelper.Factory = factoryCreator.create(tempDir, dbLogger, sqlite3Binary)

    open fun createWasmSQLiteOpenHelper(
        dbName: String? = "test.db",
        sqlite3Binary: WasmSqliteConfiguration = factoryCreator.defaultSqliteBinary,
        openHelperCallback: SupportSQLiteOpenHelper.Callback = LoggingOpenHelperCallback(dbLogger),
    ): SupportSQLiteOpenHelper {
        val factory = factoryCreator.create(tempDir, dbLogger, sqlite3Binary)
        val mockContext = ContextWrapper(null)
        val config = Configuration(mockContext, "test.db", LoggingOpenHelperCallback(dbLogger))
        return factory.create(config)
    }
}

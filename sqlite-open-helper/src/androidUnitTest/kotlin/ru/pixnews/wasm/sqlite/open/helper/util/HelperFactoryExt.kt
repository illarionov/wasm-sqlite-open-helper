/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.util

import android.content.ContextWrapper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import org.graalvm.polyglot.Engine
import ru.pixnews.wasm.sqlite.open.helper.Sqlite3Wasm.Emscripten
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperFactory
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver
import java.io.File

fun createWasmSQLiteOpenHelper(
    dstDir: File,
    dbLogger: Logger,
    dbName: String = "test.db",
    openHelperCallback: SupportSQLiteOpenHelper.Callback = LoggingOpenHelperCallback(dbLogger),
): SupportSQLiteOpenHelper {
    val factory = createWasmSqliteOpenHelperFactory(dstDir, dbLogger)
    val mockContext = ContextWrapper(null)
    val config = SupportSQLiteOpenHelper.Configuration(mockContext, dbName, openHelperCallback)
    return factory.create(config)
}

fun createWasmSqliteOpenHelperFactory(
    dstDir: File,
    dbLogger: Logger,
): SupportSQLiteOpenHelper.Factory {
    return WasmSqliteOpenHelperFactory(GraalvmSqliteEmbedder) {
        pathResolver = DatabasePathResolver { name -> File(dstDir, name) }
        logger = dbLogger
        embedder {
            graalvmEngine = Engine.create("wasm")
            sqlite3Binary = Emscripten.sqlite3_345_mt_pthread
        }
        debug {
            sqlLog = true
            sqlTime = true
            sqlStatements = true
            logSlowQueries = true
        }
    }
}

private class LoggingOpenHelperCallback(
    private val logger: Logger = Logger.withTag("SupportSQLiteOpenHelperCallback"),
    version: Int = 1,
) : SupportSQLiteOpenHelper.Callback(version) {
    override fun onCreate(db: SupportSQLiteDatabase) {
        logger.i { "onCreate() $db" }
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        logger.i { "onUpgrade() $db, $oldVersion, $newVersion" }
    }
}

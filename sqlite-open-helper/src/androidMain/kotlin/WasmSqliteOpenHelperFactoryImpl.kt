/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import androidx.sqlite.db.SupportSQLiteOpenHelper
import at.released.wasm.sqlite.open.helper.debug.SqliteErrorLogger
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementLogger
import at.released.wasm.sqlite.open.helper.debug.SqliteStatementProfileLogger
import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfig
import at.released.wasm.sqlite.open.helper.dsl.OpenParamsBlock
import at.released.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntime
import at.released.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.wasm.sqlite.open.helper.embedder.functiontable.SqliteCallbackFunctionIndexes
import at.released.wasm.sqlite.open.helper.internal.DatabaseErrorHandler
import at.released.wasm.sqlite.open.helper.internal.OpenHelperNativeBindings
import at.released.wasm.sqlite.open.helper.internal.SQLiteDatabaseOpenParams
import at.released.wasm.sqlite.open.helper.internal.WasmSqliteOpenHelper
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import at.released.weh.common.api.Logger
import at.released.weh.wasm.core.memory.Memory

/**
 * Implements [SupportSQLiteOpenHelper.Factory] using the SQLite implementation shipped in
 * this library.
 */
internal class WasmSqliteOpenHelperFactoryImpl<R : SqliteRuntime>(
    private val pathResolver: DatabasePathResolver,
    private val debugConfig: WasmSqliteDebugConfig,
    private val openParams: OpenParamsBlock,
    private val sqliteExports: SqliteExports,
    private val memory: Memory,
    private val callbackStore: SqliteCallbackStore,
    private val callbackFunctionIndexes: SqliteCallbackFunctionIndexes,
    override val runtime: R,
    rootLogger: Logger,
) : WasmSQLiteOpenHelperFactory<R> {
    private val logger: Logger = rootLogger.withTag("WasmSqliteOpenHelperFactory")

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val cApi = Sqlite3CApi(
            sqliteExports = sqliteExports,
            embedderInfo = runtime.embedderInfo,
            memory = memory,
            callbackStore = callbackStore,
            callbackFunctionIndexes = callbackFunctionIndexes,
            rootLogger = logger,
        )

        val bindings = OpenHelperNativeBindings(
            cApi,
            debugConfig.getOrCreateDefault(SqliteStatementLogger),
            debugConfig.getOrCreateDefault(SqliteStatementProfileLogger),
            debugConfig.getOrCreateDefault(SqliteErrorLogger),
            logger,
        )

        val openParamsBuilder: SQLiteDatabaseOpenParams.Builder = SQLiteDatabaseOpenParams.Builder().apply {
            errorHandler = DatabaseErrorHandler { dbObj -> configuration.callback.onCorruption(dbObj) }
            set(openParams)
        }

        return WasmSqliteOpenHelper(
            pathResolver = pathResolver,
            defaultLocale = openParams.locale,
            debugConfig = debugConfig,
            callback = configuration.callback,
            rootLogger = logger,
            databaseName = configuration.name,
            openParamsBuilder = openParamsBuilder,
            bindings = bindings,
        )
    }
}

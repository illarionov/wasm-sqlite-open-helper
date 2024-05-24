/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import androidx.sqlite.db.SupportSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import ru.pixnews.wasm.sqlite.open.helper.embedder.SQLiteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable.Sqlite3CallbackFunctionIndexes
import ru.pixnews.wasm.sqlite.open.helper.internal.DatabaseErrorHandler
import ru.pixnews.wasm.sqlite.open.helper.internal.OpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabaseOpenParams
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDebug
import ru.pixnews.wasm.sqlite.open.helper.internal.WasmSqliteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi

/**
 * Implements [SupportSQLiteOpenHelper.Factory] using the SQLite implementation shipped in
 * this library.
 */
internal class WasmSqliteOpenHelperFactory(
    private val pathResolver: DatabasePathResolver,
    private val debugConfig: SQLiteDebug,
    private val openParams: OpenParamsBlock,
    private val sqliteBindings: SqliteBindings,
    private val embedderInfo: SQLiteEmbedderRuntimeInfo,
    private val memory: EmbedderMemory,
    private val callbackStore: SqliteCallbackStore,
    private val callbackFunctionIndexes: Sqlite3CallbackFunctionIndexes,
    rootLogger: Logger,
) : SupportSQLiteOpenHelper.Factory {
    private val logger: Logger = rootLogger.withTag("WasmSqliteOpenHelperFactory")

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val cApi = Sqlite3CApi(
            sqliteBindings = sqliteBindings,
            embedderInfo = embedderInfo,
            memory = memory,
            callbackStore = callbackStore,
            callbackFunctionIndexes = callbackFunctionIndexes,
            rootLogger = logger,
        )

        val bindings = OpenHelperNativeBindings(cApi, logger)

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

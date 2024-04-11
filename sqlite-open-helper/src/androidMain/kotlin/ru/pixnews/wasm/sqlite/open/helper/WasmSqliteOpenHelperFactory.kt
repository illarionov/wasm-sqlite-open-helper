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
import ru.pixnews.wasm.sqlite.open.helper.base.DatabaseErrorHandler
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.dsl.OpenParamsBlock
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteCapi
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabaseOpenParams
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDebug
import ru.pixnews.wasm.sqlite.open.helper.internal.WasmSqliteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.GraalNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver

/**
 * Implements [SupportSQLiteOpenHelper.Factory] using the SQLite implementation shipped in
 * this library.
 */
internal class WasmSqliteOpenHelperFactory(
    private val pathResolver: DatabasePathResolver,
    private val sqliteCapi: SqliteCapi,
    private val debugConfig: SQLiteDebug,
    private val openParams: OpenParamsBlock,
    rootLogger: Logger,
) : SupportSQLiteOpenHelper.Factory {
    private val logger: Logger = rootLogger.withTag("WasmSqliteOpenHelperFactory")

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val bindings = GraalNativeBindings(sqliteCapi, logger)

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

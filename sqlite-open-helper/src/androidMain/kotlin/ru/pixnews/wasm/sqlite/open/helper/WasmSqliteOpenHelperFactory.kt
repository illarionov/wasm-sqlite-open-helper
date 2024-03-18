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
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteCapi
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabase
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDebug
import ru.pixnews.wasm.sqlite.open.helper.internal.WasmSqliteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.GraalNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.GraalWindowBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperWindowBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3WindowPtr
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver

/**
 * Implements [SupportSQLiteOpenHelper.Factory] using the SQLite implementation shipped in
 * this library.
 */
internal class WasmSqliteOpenHelperFactory(
    private val pathResolver: DatabasePathResolver,
    private val sqliteCapi: SqliteCapi,
    private val debugConfig: SQLiteDebug = SQLiteDebug(),
    private val configurationOptions: List<ConfigurationOptions> = emptyList(),
    rootLogger: Logger,
) : SupportSQLiteOpenHelper.Factory {
    private val logger: Logger = rootLogger.withTag(WasmSqliteOpenHelperFactory::class.qualifiedName!!)

    override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
        val bindings = GraalNativeBindings(sqliteCapi, logger)
        return CallbackSqliteOpenHelper(
            pathResolver = pathResolver,
            debugConfig = debugConfig,
            rootLogger = logger,
            name = configuration.name,
            cb = configuration.callback,
            ops = configurationOptions,
            bindings = bindings,
            windowBindings = GraalWindowBindings(logger),
        )
    }

    private class CallbackSqliteOpenHelper<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr, WP : Sqlite3WindowPtr>(
        pathResolver: DatabasePathResolver,
        debugConfig: SQLiteDebug,
        rootLogger: Logger,
        name: String?,
        cb: SupportSQLiteOpenHelper.Callback,
        ops: Iterable<ConfigurationOptions>,
        bindings: SqlOpenHelperNativeBindings<CP, SP, WP>,
        windowBindings: SqlOpenHelperWindowBindings<WP>,
    ) : WasmSqliteOpenHelper<CP, SP, WP>(
        pathResolver = pathResolver,
        debugConfig = debugConfig,
        rootLogger = rootLogger,
        databaseName = name,
        factory = null,
        version = cb.version,
        errorHandler = CallbackDatabaseErrorHandler(cb),
        bindings = bindings,
        windowBindings = windowBindings,
    ) {
        private val callback: SupportSQLiteOpenHelper.Callback = cb
        private val configurationOptions = ops

        override fun onConfigure(db: SQLiteDatabase<CP, SP, WP>) = callback.onConfigure(db)

        override fun onCreate(db: SQLiteDatabase<CP, SP, WP>) = callback.onCreate(db)

        override fun onUpgrade(db: SQLiteDatabase<CP, SP, WP>, oldVersion: Int, newVersion: Int) =
            callback.onUpgrade(db, oldVersion, newVersion)

        override fun onDowngrade(db: SQLiteDatabase<CP, SP, WP>, oldVersion: Int, newVersion: Int): Unit =
            callback.onDowngrade(db, oldVersion, newVersion)

        override fun onOpen(db: SQLiteDatabase<CP, SP, WP>) = callback.onOpen(db)

        override fun createConfiguration(path: String, openFlags: OpenFlags): SqliteDatabaseConfiguration {
            var config = super.createConfiguration(path, openFlags)

            configurationOptions.forEach { option ->
                config = option.apply(config)
            }

            return config
        }
    }

    private class CallbackDatabaseErrorHandler(
        private val callback: SupportSQLiteOpenHelper.Callback,
    ) : DatabaseErrorHandler {
        override fun onCorruption(dbObj: SQLiteDatabase<*, *, *>) = callback.onCorruption(dbObj)
    }
}
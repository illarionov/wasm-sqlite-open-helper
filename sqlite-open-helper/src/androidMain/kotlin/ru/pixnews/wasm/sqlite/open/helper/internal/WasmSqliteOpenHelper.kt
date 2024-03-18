/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("WRONG_ORDER_IN_CLASS_LIKE_STRUCTURES")

package ru.pixnews.wasm.sqlite.open.helper.internal

/*
 * Original Copyrights:
 * Copyright (C) 2017-2024 requery.io
 * Copyright (C) 2005-2012 The Android Open Source Project
 * Licensed under the Apache License, Version 2.0 (the "License")
 */

import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.ConfigurationOptions
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.CREATE_IF_NECESSARY
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.OPEN_READONLY
import ru.pixnews.wasm.sqlite.open.helper.SqliteDatabaseConfiguration
import ru.pixnews.wasm.sqlite.open.helper.base.DatabaseErrorHandler
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver

/**
 * A helper class to manage database creation and version management.
 *
 */
internal class WasmSqliteOpenHelper<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr>(
    private val pathResolver: DatabasePathResolver,
    private val defaultLocale: Locale,
    private val debugConfig: SQLiteDebug,
    private val callback: SupportSQLiteOpenHelper.Callback,
    private val configurationOptions: Iterable<ConfigurationOptions>,
    rootLogger: Logger,
    override val databaseName: String?,
    private val bindings: SqlOpenHelperNativeBindings<CP, SP>,
) : SupportSQLiteOpenHelper {
    private val logger: Logger = rootLogger.withTag(TAG)
    private var database: SQLiteDatabase<CP, SP>? = null
    private var isInitializing = false
    private var enableWriteAheadLogging = false
    private val version: Int get() = callback.version
    private val errorHandler: DatabaseErrorHandler = DatabaseErrorHandler { dbObj -> callback.onCorruption(dbObj) }

    init {
        require(version >= 1) { "Version must be >= 1, was $version" }
    }

    /**
     * Create and/or open a database that will be used for reading and writing.
     * The first time this is called, the database will be opened and
     * [.onCreate], [.onUpgrade] and/or [.onOpen] will be
     * called.
     *
     *
     * Once opened successfully, the database is cached, so you can
     * call this method every time you need to write to the database.
     * (Make sure to call [.close] when you no longer need the database.)
     * Errors such as bad permissions or a full disk may cause this method
     * to fail, but future attempts may succeed if the problem is fixed.
     *
     *
     * Database upgrade may take a long time, you
     * should not call this method from the application main thread, including
     * from [ContentProvider.onCreate()][android.content.ContentProvider.onCreate].
     *
     * @return a read/write database object valid until [.close] is called
     * @throws SQLiteException if the database cannot be opened for writing
     */
    override val writableDatabase: SQLiteDatabase<CP, SP>
        get() = synchronized(this) {
            getDatabaseLocked(true)
        }

    /**
     * Create and/or open a database.  This will be the same object returned by
     * [.getWritableDatabase] unless some problem, such as a full disk,
     * requires the database to be opened read-only.  In that case, a read-only
     * database object will be returned.  If the problem is fixed, a future call
     * to [.getWritableDatabase] may succeed, in which case the read-only
     * database object will be closed and the read/write object will be returned
     * in the future.
     *
     *
     * Like [.getWritableDatabase], this method may
     * take a long time to return, so you should not call it from the
     * application main thread, including from
     * [ContentProvider.onCreate()][android.content.ContentProvider.onCreate].
     *
     * @return a database object valid until [.getWritableDatabase]
     * @throws SQLiteException if the database cannot be opened
     * or [.close] is called.
     */
    override val readableDatabase: SQLiteDatabase<CP, SP>
        get() = synchronized(this) {
            getDatabaseLocked(false)
        }

    /**
     * Enables or disables the use of write-ahead logging for the database.
     *
     * Write-ahead logging cannot be used with read-only databases so the value of
     * this flag is ignored if the database is opened read-only.
     *
     * @param enabled True if write-ahead logging should be enabled, false if it
     * should be disabled.
     *
     * @see SQLiteDatabase.enableWriteAheadLogging
     */
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        synchronized(this) {
            if (enableWriteAheadLogging != enabled) {
                if (database != null && database!!.isOpen && !database!!.isReadOnly) {
                    if (enabled) {
                        database!!.enableWriteAheadLogging()
                    } else {
                        database!!.disableWriteAheadLogging()
                    }
                }
                enableWriteAheadLogging = enabled
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    private fun getDatabaseLocked(writable: Boolean): SQLiteDatabase<CP, SP> {
        database?.let { db ->
            if (!db.isOpen) {
                // Darn!  The user closed the database by calling mDatabase.close().
                database = null
            } else if (!writable || !db.isReadOnly) {
                // The database is already open for business.
                return db
            }
        }

        check(!isInitializing) { "getDatabase called recursively" }

        var db = database
        try {
            isInitializing = true

            if (db != null) {
                if (db.isReadOnly) {
                    db.reopenReadWrite()
                }
            } else if (databaseName == null) {
                db = SQLiteDatabase.create(
                    bindings = bindings,
                    debugConfig = debugConfig,
                    locale = defaultLocale,
                    logger = logger,
                )
            } else {
                try {
                    val path = pathResolver.getDatabasePath(databaseName.toString()).path
                    if (DEBUG_STRICT_READONLY && !writable) {
                        val configuration = createConfiguration(path, defaultLocale, OPEN_READONLY)
                        db = SQLiteDatabase.openDatabase(
                            configuration = configuration,
                            errorHandler = errorHandler,
                            bindings = bindings,
                            debugConfig = debugConfig,
                            logger = logger,
                        )
                    } else {
                        var flags = if (enableWriteAheadLogging) ENABLE_WRITE_AHEAD_LOGGING else OpenFlags(0U)
                        flags = flags or CREATE_IF_NECESSARY
                        val configuration = createConfiguration(path, defaultLocale, flags)
                        db = SQLiteDatabase.openDatabase(
                            configuration = configuration,
                            errorHandler = errorHandler,
                            bindings = bindings,
                            debugConfig = debugConfig,
                            logger = logger,
                        )
                    }
                } catch (ex: SQLiteException) {
                    if (writable) {
                        throw ex
                    }
                    logger.e(ex) { "Couldn't open $databaseName for writing (will try read-only):" }
                    val path = pathResolver.getDatabasePath(databaseName.toString()).path
                    val configuration = createConfiguration(path, defaultLocale, OPEN_READONLY)
                    db = SQLiteDatabase.openDatabase(
                        configuration = configuration,
                        errorHandler = errorHandler,
                        bindings = bindings,
                        debugConfig = debugConfig,
                        logger = logger,
                    )
                }
            }

            callback.onConfigure(db!!)

            val version = db.version
            if (version != this.version) {
                if (db.isReadOnly) {
                    throw SQLiteException(
                        "Can't upgrade read-only database from version ${db.version} to ${this.version}: $databaseName",
                    )
                }

                db.beginTransaction()
                try {
                    if (version == 0) {
                        callback.onCreate(db)
                    } else {
                        if (version > this.version) {
                            callback.onDowngrade(db, version, this.version)
                        } else {
                            callback.onUpgrade(db, version, this.version)
                        }
                    }
                    db.version = this.version
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            callback.onOpen(db)

            if (db.isReadOnly) {
                logger.w { "Opened $databaseName in read-only mode" }
            }

            database = db
            return db
        } finally {
            isInitializing = false
            if (db != null && db != database) {
                db.close()
            }
        }
    }

    /**
     * Close any open database object.
     */
    @Synchronized
    override fun close() {
        check(!isInitializing) { "Closed during initialization" }
        database?.let { db ->
            if (db.isOpen) {
                db.close()
                database = null
            }
        }
    }

    /**
     * Called before the database is opened. Provides the [SqliteDatabaseConfiguration]
     * instance that is used to initialize the database. Override this to create a configuration
     * that has custom functions or extensions.
     *
     * @param path to database file to open and/or create
     * @param openFlags to control database access mode
     * @return [SqliteDatabaseConfiguration] instance, cannot be null.
     */
    private fun createConfiguration(
        path: String,
        defaultLocale: Locale,
        openFlags: OpenFlags,
    ): SqliteDatabaseConfiguration {
        var config = SqliteDatabaseConfiguration(path, openFlags, defaultLocale)
        configurationOptions.forEach { option ->
            config = option.apply(config)
        }
        return config
    }

    companion object {
        // When true, getReadableDatabase returns a read-only database if it is just being opened.
        // The database handle is reopened in read/write mode when getWritableDatabase is called.
        // We leave this behavior disabled in production because it is inefficient and breaks
        // many applications.  For debugging purposes it can be useful to turn on strict
        // read-only semantics to catch applications that call getReadableDatabase when they really
        // wanted getWritableDatabase.
        private const val DEBUG_STRICT_READONLY = false
        private val TAG: String = WasmSqliteOpenHelper::class.qualifiedName!!
    }
}

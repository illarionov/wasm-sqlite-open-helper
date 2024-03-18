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
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.CREATE_IF_NECESSARY
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.ENABLE_WRITE_AHEAD_LOGGING
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.OPEN_READONLY
import ru.pixnews.wasm.sqlite.open.helper.SqliteDatabaseConfiguration
import ru.pixnews.wasm.sqlite.open.helper.base.DatabaseErrorHandler
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.or
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDatabase.CursorFactory
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.SqlOpenHelperNativeBindings
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3ConnectionPtr
import ru.pixnews.wasm.sqlite.open.helper.internal.interop.Sqlite3StatementPtr
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver

/**
 * A helper class to manage database creation and version management.
 *
 */
internal abstract class WasmSqliteOpenHelper<CP : Sqlite3ConnectionPtr, SP : Sqlite3StatementPtr>(
    private val pathResolver: DatabasePathResolver,
    private val defaultLocale: Locale,
    private val debugConfig: SQLiteDebug,
    rootLogger: Logger,
    override val databaseName: String?,
    private val factory: CursorFactory<CP, SP>?,
    private val version: Int,
    private val errorHandler: DatabaseErrorHandler? = null,
    private val bindings: SqlOpenHelperNativeBindings<CP, SP>,
) : SupportSQLiteOpenHelper {
    private val logger: Logger = rootLogger.withTag(TAG)
    private var database: SQLiteDatabase<CP, SP>? = null
    private var isInitializing = false
    private var enableWriteAheadLogging = false

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
                    factory = null,
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
                            factory = factory,
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
                            factory = factory,
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
                        factory = factory,
                        errorHandler = errorHandler,
                        bindings = bindings,
                        debugConfig = debugConfig,
                        logger = logger,
                    )
                }
            }

            onConfigure(db!!)

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
                        onCreate(db)
                    } else {
                        if (version > this.version) {
                            onDowngrade(db, version, this.version)
                        } else {
                            onUpgrade(db, version, this.version)
                        }
                    }
                    db.version = this.version
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }

            onOpen(db)

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
     * Called when the database connection is being configured, to enable features
     * such as write-ahead logging or foreign key support.
     *
     *
     * This method is called before [.onCreate], [.onUpgrade],
     * [.onDowngrade], or [.onOpen] are called.  It should not modify
     * the database except to configure the database connection as required.
     *
     *
     * This method should only call methods that configure the parameters of the
     * database connection, such as [SQLiteDatabase.enableWriteAheadLogging]
     * [SQLiteDatabase.setForeignKeyConstraintsEnabled],
     * [SQLiteDatabase.setLocale], [SQLiteDatabase.setMaximumSize],
     * or executing PRAGMA statements.
     *
     *
     * @param db The database.
     */
    open fun onConfigure(db: SQLiteDatabase<CP, SP>) {}

    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    abstract fun onCreate(db: SQLiteDatabase<CP, SP>)

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     *
     *
     *
     * The SQLite ALTER TABLE documentation can be found
     * [here](http://sqlite.org/lang_altertable.html). If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     *
     *
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     *
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    abstract fun onUpgrade(db: SQLiteDatabase<CP, SP>, oldVersion: Int, newVersion: Int)

    /**
     * Called when the database needs to be downgraded. This is strictly similar to
     * [.onUpgrade] method, but is called whenever current version is newer than requested one.
     * However, this method is not abstract, so it is not mandatory for a customer to
     * implement it. If not overridden, default implementation will reject downgrade and
     * throws SQLiteException
     *
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     *
     * @param db The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     * @throws SQLiteException
     */
    open fun onDowngrade(db: SQLiteDatabase<CP, SP>, oldVersion: Int, newVersion: Int) {
        throw SQLiteException("Can't downgrade database from version $oldVersion to $newVersion")
    }

    /**
     * Called when the database has been opened.  The implementation
     * should check [SQLiteDatabase.isReadOnly] before updating the
     * database.
     *
     *
     * This method is called after the database connection has been configured
     * and after the database schema has been created, upgraded or downgraded as necessary.
     * If the database connection must be configured in some way before the schema
     * is created, upgraded, or downgraded, do it in [.onConfigure] instead.
     *
     *
     * @param db The database.
     */
    open fun onOpen(db: SQLiteDatabase<CP, SP>) {}

    /**
     * Called before the database is opened. Provides the [SqliteDatabaseConfiguration]
     * instance that is used to initialize the database. Override this to create a configuration
     * that has custom functions or extensions.
     *
     * @param path to database file to open and/or create
     * @param openFlags to control database access mode
     * @return [SqliteDatabaseConfiguration] instance, cannot be null.
     */
    protected open fun createConfiguration(
        path: String,
        defaultLocale: Locale,
        openFlags: OpenFlags,
    ): SqliteDatabaseConfiguration = SqliteDatabaseConfiguration(path, openFlags, defaultLocale)

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

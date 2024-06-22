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
import ru.pixnews.wasm.sqlite.open.helper.OpenFlags.Companion.OPEN_READONLY
import ru.pixnews.wasm.sqlite.open.helper.common.api.Locale
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfig
import ru.pixnews.wasm.sqlite.open.helper.dsl.path.DatabasePathResolver
import java.nio.file.FileSystems
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.setPosixFilePermissions

/**
 * A helper class to manage database creation and version management.
 */
internal class WasmSqliteOpenHelper(
    private val pathResolver: DatabasePathResolver,
    private val defaultLocale: Locale,
    private val debugConfig: WasmSqliteDebugConfig,
    private val callback: SupportSQLiteOpenHelper.Callback,
    private val openParamsBuilder: SQLiteDatabaseOpenParams.Builder,
    rootLogger: Logger,
    override val databaseName: String?,
    private val bindings: OpenHelperNativeBindings,
) : SupportSQLiteOpenHelper {
    private val logger: Logger = rootLogger.withTag("WasmSqliteOpenHelper")
    private val version: Int get() = callback.version

    private var isInitializing = false
    private var sqliteInitialized: Boolean = false
    private var database: SQLiteDatabase? = null

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
    override val writableDatabase: SQLiteDatabase
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
    override val readableDatabase: SQLiteDatabase
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
    @Synchronized
    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        if (openParamsBuilder.isWriteAheadLoggingEnabled != enabled) {
            database?.let {
                if (it.isOpen && !it.isReadOnly) {
                    if (enabled) {
                        it.enableWriteAheadLogging()
                    } else {
                        it.disableWriteAheadLogging()
                    }
                }
            }
            openParamsBuilder.isWriteAheadLoggingEnabled = enabled
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    private fun getDatabaseLocked(writable: Boolean): SQLiteDatabase {
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

            if (!sqliteInitialized) {
                sqliteInitialized = true
                bindings.nativeInit()
            }

            if (db != null) {
                if (db.isReadOnly) {
                    db.reopenReadWrite()
                }
            } else if (databaseName == null) {
                db = SQLiteDatabase.createInMemory(
                    bindings = bindings,
                    debugConfig = debugConfig,
                    openParams = openParamsBuilder.build(),
                    logger = logger,
                )
            } else {
                try {
                    val path = pathResolver.getDatabasePath(databaseName.toString())
                    val openParams = openParamsBuilder.build()
                    db = SQLiteDatabase.openDatabase(
                        path = path,
                        defaultLocale = defaultLocale,
                        openParams = openParams,
                        bindings = bindings,
                        debugConfig = debugConfig,
                        logger = logger,
                    )
                    // Keep pre-O-MR1 behavior by resetting file permissions to 660
                    setFilePermissionsForDb(path)
                } catch (ex: SQLiteException) {
                    if (writable) {
                        throw ex
                    }
                    logger.e(ex) { "Couldn't open $databaseName for writing (will try read-only):" }
                    val path = pathResolver.getDatabasePath(databaseName.toString())
                    val openParams = openParamsBuilder.build().toBuilder().apply {
                        addOpenFlags(OPEN_READONLY)
                    }.build()
                    db = SQLiteDatabase.openDatabase(
                        path = path,
                        defaultLocale = defaultLocale,
                        openParams = openParams,
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

    private companion object {
        private fun setFilePermissionsForDb(dbPath: String) {
            try {
                FileSystems.getDefault().getPath(dbPath).setPosixFilePermissions(
                    PosixFilePermissions.fromString("rw-rw----"),
                )
            } catch (ignore: Exception) {
                // Ignore
            }
        }
    }
}

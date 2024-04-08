/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnusedPrivateProperty")

package ru.pixnews.wasm.sqlite.open.helper.exception

public expect open class AndroidSqlException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
}

public expect open class AndroidSqliteException : AndroidSqlException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
}

public expect open class AndroidSqliteDiskIoException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteDatabaseCorruptException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteConstraintException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteAbortException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteDoneException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteFullException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteMisuseException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteAccessPermException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteDatabaseLockedException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteTableLockedException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteReadOnlyDatabaseException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteCantOpenDatabaseException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteBlobTooBigException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteBindOrColumnIndexOutOfRangeException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteOutOfMemoryException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidSqliteDatatypeMismatchException : AndroidSqliteException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidOperationCanceledException : RuntimeException {
    public constructor()
    public constructor(message: String?)
}

public expect open class AndroidCursorWindowAllocationException public constructor(message: String) : RuntimeException

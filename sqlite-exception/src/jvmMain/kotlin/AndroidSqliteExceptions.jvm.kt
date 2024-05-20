/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.exception

public actual open class AndroidSqlException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

public actual open class AndroidSqliteException : AndroidSqlException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
    public actual constructor(message: String?, cause: Throwable?) : super(message, cause)
}

public actual open class AndroidSqliteDiskIoException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteDatabaseCorruptException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteConstraintException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteAbortException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteDoneException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteFullException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteMisuseException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteAccessPermException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteDatabaseLockedException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteTableLockedException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteReadOnlyDatabaseException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteCantOpenDatabaseException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteBlobTooBigException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteBindOrColumnIndexOutOfRangeException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteOutOfMemoryException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidSqliteDatatypeMismatchException : AndroidSqliteException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidOperationCanceledException : RuntimeException {
    public actual constructor() : super()
    public actual constructor(message: String?) : super(message)
}

public actual open class AndroidCursorWindowAllocationException public actual constructor(
    message: String,
) : RuntimeException(message)

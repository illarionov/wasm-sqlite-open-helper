/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.internal.ext

import at.released.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import at.released.wasm.sqlite.open.helper.exception.AndroidOperationCanceledException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteAbortException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteAccessPermException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteBindOrColumnIndexOutOfRangeException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteBlobTooBigException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteCantOpenDatabaseException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteConstraintException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteDatabaseCorruptException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteDatabaseLockedException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteDatatypeMismatchException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteDiskIoException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteDoneException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteFullException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteMisuseException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteOutOfMemoryException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteReadOnlyDatabaseException
import at.released.wasm.sqlite.open.helper.exception.AndroidSqliteTableLockedException
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrorInfo
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode.Companion.SQLITE_OK

@InternalWasmSqliteHelperApi
public fun throwAndroidSqliteException(
    message: String?,
    errorCode: SqliteResultCode = SQLITE_OK,
): Nothing = throwAndroidSqliteException(
    SqliteErrorInfo(errorCode, errorCode, null),
    message,
)

@InternalWasmSqliteHelperApi
@Suppress("CyclomaticComplexMethod")
public fun throwAndroidSqliteException(
    errorInfo: SqliteErrorInfo,
    message: String?,
    cause: Throwable? = null,
): Nothing {
    val fullErMsg = if (errorInfo.sqliteMsg != null) {
        buildString {
            append("Error ", errorInfo.sqliteErrorCode.toString())
            append(" (code ", errorInfo.sqliteExtendedErrorCode, ")")
            val msgs = listOfNotNull(message, errorInfo.sqliteMsg)
            if (msgs.isNotEmpty()) {
                msgs.joinTo(
                    buffer = this,
                    separator = ", ",
                    prefix = ": ",
                )
            }
            if (cause != null) {
                append("; ")
                append(cause.toString())
            }
        }
    } else {
        message
    }

    val androidException = when (errorInfo.sqliteErrorCode) {
        SqliteResultCode.SQLITE_IOERR -> AndroidSqliteDiskIoException(fullErMsg)
        SqliteResultCode.SQLITE_CORRUPT, SqliteResultCode.SQLITE_NOTADB ->
            AndroidSqliteDatabaseCorruptException(fullErMsg)

        SqliteResultCode.SQLITE_CONSTRAINT -> AndroidSqliteConstraintException(fullErMsg)
        SqliteResultCode.SQLITE_ABORT -> AndroidSqliteAbortException(fullErMsg)
        SqliteResultCode.SQLITE_DONE -> AndroidSqliteDoneException(fullErMsg)
        SqliteResultCode.SQLITE_FULL -> AndroidSqliteFullException(fullErMsg)
        SqliteResultCode.SQLITE_MISUSE -> AndroidSqliteMisuseException(fullErMsg)
        SqliteResultCode.SQLITE_PERM -> AndroidSqliteAccessPermException(fullErMsg)
        SqliteResultCode.SQLITE_BUSY -> AndroidSqliteDatabaseLockedException(fullErMsg)
        SqliteResultCode.SQLITE_LOCKED -> AndroidSqliteTableLockedException(fullErMsg)
        SqliteResultCode.SQLITE_READONLY -> AndroidSqliteReadOnlyDatabaseException(fullErMsg)
        SqliteResultCode.SQLITE_CANTOPEN -> AndroidSqliteCantOpenDatabaseException(fullErMsg)
        SqliteResultCode.SQLITE_TOOBIG -> AndroidSqliteBlobTooBigException(fullErMsg)
        SqliteResultCode.SQLITE_RANGE -> AndroidSqliteBindOrColumnIndexOutOfRangeException(fullErMsg)
        SqliteResultCode.SQLITE_NOMEM -> AndroidSqliteOutOfMemoryException(fullErMsg)
        SqliteResultCode.SQLITE_MISMATCH -> AndroidSqliteDatatypeMismatchException(fullErMsg)
        SqliteResultCode.SQLITE_INTERRUPT -> AndroidOperationCanceledException(fullErMsg)
        else -> AndroidSqliteException(fullErMsg, cause)
    }
    throw androidException
}

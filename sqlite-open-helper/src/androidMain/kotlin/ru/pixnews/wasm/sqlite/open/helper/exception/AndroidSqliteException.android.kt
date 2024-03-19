/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.exception

import android.database.SQLException
import android.database.sqlite.SQLiteAbortException
import android.database.sqlite.SQLiteAccessPermException
import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException
import android.database.sqlite.SQLiteBlobTooBigException
import android.database.sqlite.SQLiteCantOpenDatabaseException
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteDatabaseLockedException
import android.database.sqlite.SQLiteDatatypeMismatchException
import android.database.sqlite.SQLiteDiskIOException
import android.database.sqlite.SQLiteDoneException
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteMisuseException
import android.database.sqlite.SQLiteOutOfMemoryException
import android.database.sqlite.SQLiteReadOnlyDatabaseException
import android.database.sqlite.SQLiteTableLockedException
import android.os.OperationCanceledException

public actual typealias AndroidSqlException = SQLException
public actual typealias AndroidSqliteException = SQLiteException
public actual typealias AndroidSqliteDiskIoException = SQLiteDiskIOException
public actual typealias AndroidSqliteDatabaseCorruptException = SQLiteDatabaseCorruptException
public actual typealias AndroidSqliteConstraintException = SQLiteConstraintException
public actual typealias AndroidSqliteAbortException = SQLiteAbortException
public actual typealias AndroidSqliteDoneException = SQLiteDoneException
public actual typealias AndroidSqliteFullException = SQLiteFullException
public actual typealias AndroidSqliteMisuseException = SQLiteMisuseException
public actual typealias AndroidSqliteAccessPermException = SQLiteAccessPermException
public actual typealias AndroidSqliteDatabaseLockedException = SQLiteDatabaseLockedException
public actual typealias AndroidSqliteTableLockedException = SQLiteTableLockedException
public actual typealias AndroidSqliteReadOnlyDatabaseException = SQLiteReadOnlyDatabaseException
public actual typealias AndroidSqliteCantOpenDatabaseException = SQLiteCantOpenDatabaseException
public actual typealias AndroidSqliteBlobTooBigException = SQLiteBlobTooBigException
public actual typealias AndroidSqliteBindOrColumnIndexOutOfRangeException = SQLiteBindOrColumnIndexOutOfRangeException
public actual typealias AndroidSqliteOutOfMemoryException = SQLiteOutOfMemoryException
public actual typealias AndroidSqliteDatatypeMismatchException = SQLiteDatatypeMismatchException
public actual typealias AndroidOperationCanceledException = OperationCanceledException

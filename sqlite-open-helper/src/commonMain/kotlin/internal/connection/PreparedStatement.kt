/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.internal.connection

import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.internal.SQLiteStatementType.Companion.ExtendedStatementType
import at.released.wasm.sqlite.open.helper.internal.SQLiteStatementType.STATEMENT_SELECT
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteStatement

/**
 * Holder type for a prepared statement.
 *
 * Although this object holds a pointer to a native statement object, it
 * does not have a finalizer.  This is deliberate.  The [SQLiteConnection]
 * owns the statement object and will take care of freeing it when needed.
 * In particular, closing the connection requires a guarantee of deterministic
 * resource disposal because all native statement objects must be freed before
 * the native database object can be closed.  So no finalizers here.
 *
 * @property sql The SQL from which the statement was prepared.
 * @property statementPtr Lifetime is managed explicitly by the connection.
 *   The native sqlite3_stmt object pointer.
 * @property numParameters The number of parameters that the prepared statement has.
 * @property type The statement type.
 * @property readOnly True if the statement is read-only.
 * @property inCache True if the statement is in the cache.
 * @property inUse in use statements from being finalized until they are no longer in use.
 *   possible for SQLite calls to be re-entrant. Consequently we need to prevent
 *   We need this flag because due to the use of custom functions in triggers, it's
 * @property seqNum The database schema ID at the time this statement was created.  The ID is left zero for
 *   statements that are not cached.  This value is meaningful only if inCache is true.
 */
internal data class PreparedStatement(
    val sql: String,
    val statementPtr: WasmPtr<SqliteStatement>,
    val numParameters: Int = 0,
    val type: ExtendedStatementType = ExtendedStatementType.PublicType(STATEMENT_SELECT),
    val readOnly: Boolean = false,
    var inCache: Boolean = false,
    var inUse: Boolean = false,
    var seqNum: Long = 0,
)

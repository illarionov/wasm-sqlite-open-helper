/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.dsl

import ru.pixnews.wasm.sqlite.driver.internal.SqliteDebug
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl

/**
 * Debugging options
 */
@WasmSqliteOpenHelperDsl
public class DebugConfigBlock {
    /**
     * Controls the printing of SQL statements as they are executed.
     */
    public var logSqlStatements: Boolean = false

    /**
     * Controls the printing of wall-clock time taken to execute SQL statements as they are executed.
     */
    public var logSqlTime: Boolean = false

    internal fun build(): SqliteDebug = SqliteDebug(
        logSqlStatements = logSqlStatements,
        logSqlTime = logSqlTime,
    )
}

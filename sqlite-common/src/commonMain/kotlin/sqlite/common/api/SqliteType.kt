/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import at.released.weh.host.base.WasmPtr
import kotlin.time.Duration

public class SqliteDb private constructor()
public class SqliteStatement private constructor()

public typealias SqliteExecCallback = (
    results: List<String>, // **char
    columnNames: List<String>, // **char
) -> Int

public typealias SqliteComparatorCallback = (
    stringA: String,
    stringB: String,
) -> Int

public typealias SqliteComparatorCallbackRaw = (
    firstStringLength: Int,
    firstStringPtr: WasmPtr<Byte>,
    secondStringLength: Int,
    secondStringPtr: WasmPtr<Byte>,
) -> Int

public typealias SqliteTraceCallback = (trace: SqliteTrace) -> Unit

public typealias SqliteProgressCallback = (db: WasmPtr<SqliteDb>) -> Int

public typealias SqliteLogCallback = (errCode: Int, message: String) -> Unit

public sealed class SqliteTrace {
    public class TraceStmt(
        public val db: WasmPtr<SqliteDb>,
        public val statement: WasmPtr<SqliteStatement>,
        public val unexpandedSql: String?,
    ) : SqliteTrace()

    public class TraceProfile(
        public val db: WasmPtr<SqliteDb>,
        public val statement: WasmPtr<SqliteStatement>,
        public val time: Duration,
    ) : SqliteTrace()

    public class TraceRow(
        public val db: WasmPtr<SqliteDb>,
        public val statement: WasmPtr<SqliteStatement>,
    ) : SqliteTrace()

    public class TraceClose(
        public val db: WasmPtr<SqliteDb>,
    ) : SqliteTrace()
}

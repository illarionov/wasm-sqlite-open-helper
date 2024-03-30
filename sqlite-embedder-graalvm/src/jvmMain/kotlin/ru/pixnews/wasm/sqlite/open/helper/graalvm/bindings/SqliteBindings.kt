/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.member
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.GraalHostMemoryImpl
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteException

@Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")
internal class SqliteBindings(
    val context: Context,
    envBindings: Value,
    mainBindings: Value,
) {
    val _initialize: Value? by mainBindings.member()
    val __errno_location: Value? by mainBindings.member()
    val __wasm_call_ctors by mainBindings.member()

    val sqlite3_db_status by mainBindings.member()
    val sqlite3_initialize by mainBindings.member()

    val sqlite3_prepare_v2 by mainBindings.member()
    val sqlite3_step by mainBindings.member()
    val sqlite3_column_int64 by mainBindings.member()
    val sqlite3_finalize by mainBindings.member()
    val sqlite3_reset by mainBindings.member()
    val sqlite3_clear_bindings by mainBindings.member()
    val sqlite3_column_count by mainBindings.member()
    val sqlite3_column_bytes by mainBindings.member()
    val sqlite3_column_double by mainBindings.member()
    val sqlite3_column_text by mainBindings.member()
    val sqlite3_column_type by mainBindings.member()
    val sqlite3_column_name by mainBindings.member()
    val sqlite3_bind_blob by mainBindings.member()
    val sqlite3_bind_double by mainBindings.member()
    val sqlite3_bind_int64 by mainBindings.member()
    val sqlite3_bind_text by mainBindings.member()
    val sqlite3_bind_parameter_count by mainBindings.member()
    val sqlite3_stmt_readonly by mainBindings.member()
    val sqlite3_expanded_sql by mainBindings.member()
    val sqlite3_errmsg by mainBindings.member()
    val sqlite3_exec by mainBindings.member()
    val sqlite3_libversion by mainBindings.member()
    val sqlite3_libversion_number by mainBindings.member()
    val sqlite3_last_insert_rowid by mainBindings.member()
    val sqlite3_changes by mainBindings.member()
    val sqlite3_close_v2 by mainBindings.member()
    val sqlite3_progress_handler by mainBindings.member()
    val sqlite3_busy_timeout by mainBindings.member()
    val sqlite3_trace_v2 by mainBindings.member()
    val sqlite3_errcode by mainBindings.member()
    val sqlite3_extended_errcode by mainBindings.member()
    val sqlite3_open by mainBindings.member()
    val sqlite3_open_v2 by mainBindings.member()
    val sqlite3_create_collation_v2 by mainBindings.member()
    val sqlite3_db_readonly by mainBindings.member()

    val sqlite3_sourceid by mainBindings.member()

    val sqlite3_wasm_enum_json = mainBindings.getMember("sqlite3__wasm_enum_json") // 661
        ?: mainBindings.getMember("sqlite3_wasm_enum_json") // 661

    val register_localized_collators by mainBindings.member()
    val register_android_functions by mainBindings.member()

    private val memory = GraalHostMemoryImpl(envBindings.getMember("memory"))

    val memoryBindings = SqliteMemoryBindings(mainBindings, memory)

    init {
        initSqlite()
    }

    private fun initSqlite() {
         requireNotNull(__wasm_call_ctors) {
             "__wasm_call_ctors not defined"
         }.execute()
        memoryBindings.init()
        postRun()
    }

    private fun postRun() {
        val sqliteInitResult = sqlite3_initialize.execute().asInt()
        if (sqliteInitResult != SqliteErrno.SQLITE_OK.id) {
            throw SqliteException(sqliteInitResult, sqliteInitResult)
        }
    }
}

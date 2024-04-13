/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.member
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

@Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")
internal class SqliteBindings(
    sqliteBindings: Value,
    private val memory: Memory,
) {
    val _initialize: Value? by sqliteBindings.member()
    val __errno_location: Value? by sqliteBindings.member()
    val __wasm_call_ctors by sqliteBindings.member()

    val sqlite3_db_status by sqliteBindings.member()
    val sqlite3_initialize by sqliteBindings.member()

    val sqlite3_prepare_v2 by sqliteBindings.member()
    val sqlite3_step by sqliteBindings.member()
    val sqlite3_column_int64 by sqliteBindings.member()
    val sqlite3_finalize by sqliteBindings.member()
    val sqlite3_reset by sqliteBindings.member()
    val sqlite3_clear_bindings by sqliteBindings.member()
    val sqlite3_column_count by sqliteBindings.member()
    val sqlite3_column_bytes by sqliteBindings.member()
    val sqlite3_column_double by sqliteBindings.member()
    val sqlite3_column_text by sqliteBindings.member()
    val sqlite3_column_type by sqliteBindings.member()
    val sqlite3_column_name by sqliteBindings.member()
    val sqlite3_bind_blob by sqliteBindings.member()
    val sqlite3_bind_double by sqliteBindings.member()
    val sqlite3_bind_int64 by sqliteBindings.member()
    val sqlite3_bind_text by sqliteBindings.member()
    val sqlite3_bind_parameter_count by sqliteBindings.member()
    val sqlite3_stmt_readonly by sqliteBindings.member()
    val sqlite3_expanded_sql by sqliteBindings.member()
    val sqlite3_errmsg by sqliteBindings.member()
    val sqlite3_exec by sqliteBindings.member()
    val sqlite3_libversion by sqliteBindings.member()
    val sqlite3_libversion_number by sqliteBindings.member()
    val sqlite3_last_insert_rowid by sqliteBindings.member()
    val sqlite3_changes by sqliteBindings.member()
    val sqlite3_close_v2 by sqliteBindings.member()
    val sqlite3_progress_handler by sqliteBindings.member()
    val sqlite3_soft_heap_limit64 by sqliteBindings.member()
    val sqlite3_busy_timeout by sqliteBindings.member()
    val sqlite3_trace_v2 by sqliteBindings.member()
    val sqlite3_errcode by sqliteBindings.member()
    val sqlite3_extended_errcode by sqliteBindings.member()
    val sqlite3_open by sqliteBindings.member()
    val sqlite3_open_v2 by sqliteBindings.member()
    val sqlite3_create_collation_v2 by sqliteBindings.member()
    val sqlite3_db_readonly by sqliteBindings.member()

    val sqlite3_sourceid by sqliteBindings.member()

    val sqlite3__wasm_enum_json by sqliteBindings.member()
    val sqlite3__wasm_config_i by sqliteBindings.member()
    val sqlite3__wasm_config_ii by sqliteBindings.member()
    val sqlite3__wasm_config_j by sqliteBindings.member()
    val sqlite3__wasm_db_config_ip by sqliteBindings.member()
    val sqlite3__wasm_db_config_pii by sqliteBindings.member()
    val sqlite3__wasm_db_config_s by sqliteBindings.member()

    val register_localized_collators by sqliteBindings.member()
    val register_android_functions by sqliteBindings.member()

    val memoryBindings = SqliteMemoryBindings(sqliteBindings)

    init {
        initSqlite()
    }

    private fun initSqlite() {
         __wasm_call_ctors.execute()
        memoryBindings.init(memory)
    }
}

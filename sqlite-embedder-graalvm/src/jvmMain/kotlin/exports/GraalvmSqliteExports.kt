/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.exports

import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.functionMember

@Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")
internal class GraalvmSqliteExports(
    sqliteBindings: () -> Value,
) : SqliteExports {
    override val sqlite3_db_status by sqliteBindings.functionMember()
    override val sqlite3_initialize by sqliteBindings.functionMember()

    override val sqlite3_prepare_v2 by sqliteBindings.functionMember()
    override val sqlite3_step by sqliteBindings.functionMember()
    override val sqlite3_column_int64 by sqliteBindings.functionMember()
    override val sqlite3_finalize by sqliteBindings.functionMember()
    override val sqlite3_reset by sqliteBindings.functionMember()
    override val sqlite3_clear_bindings by sqliteBindings.functionMember()
    override val sqlite3_column_count by sqliteBindings.functionMember()
    override val sqlite3_column_bytes by sqliteBindings.functionMember()
    override val sqlite3_column_double by sqliteBindings.functionMember()
    override val sqlite3_column_text by sqliteBindings.functionMember()
    override val sqlite3_column_type by sqliteBindings.functionMember()
    override val sqlite3_column_name by sqliteBindings.functionMember()
    override val sqlite3_bind_blob by sqliteBindings.functionMember()
    override val sqlite3_bind_double by sqliteBindings.functionMember()
    override val sqlite3_bind_int64 by sqliteBindings.functionMember()
    override val sqlite3_bind_null by sqliteBindings.functionMember()
    override val sqlite3_bind_text by sqliteBindings.functionMember()
    override val sqlite3_bind_parameter_count by sqliteBindings.functionMember()
    override val sqlite3_stmt_readonly by sqliteBindings.functionMember()
    override val sqlite3_expanded_sql by sqliteBindings.functionMember()
    override val sqlite3_errmsg by sqliteBindings.functionMember()
    override val sqlite3_libversion by sqliteBindings.functionMember()
    override val sqlite3_libversion_number by sqliteBindings.functionMember()
    override val sqlite3_last_insert_rowid by sqliteBindings.functionMember()
    override val sqlite3_changes by sqliteBindings.functionMember()
    override val sqlite3_close_v2 by sqliteBindings.functionMember()
    override val sqlite3_progress_handler by sqliteBindings.functionMember()
    override val sqlite3_soft_heap_limit64 by sqliteBindings.functionMember()
    override val sqlite3_busy_timeout by sqliteBindings.functionMember()
    override val sqlite3_trace_v2 by sqliteBindings.functionMember()
    override val sqlite3_errcode by sqliteBindings.functionMember()
    override val sqlite3_extended_errcode by sqliteBindings.functionMember()
    override val sqlite3_open by sqliteBindings.functionMember()
    override val sqlite3_open_v2 by sqliteBindings.functionMember()
    override val sqlite3_db_readonly by sqliteBindings.functionMember()

    override val sqlite3_sourceid by sqliteBindings.functionMember()

    override val sqlite3__wasm_enum_json by sqliteBindings.functionMember()
    override val sqlite3__wasm_config_i by sqliteBindings.functionMember()
    override val sqlite3__wasm_config_ii by sqliteBindings.functionMember()
    override val sqlite3__wasm_config_j by sqliteBindings.functionMember()
    override val sqlite3__wasm_db_config_ip by sqliteBindings.functionMember()
    override val sqlite3__wasm_db_config_pii by sqliteBindings.functionMember()
    override val sqlite3__wasm_db_config_s by sqliteBindings.functionMember()

    override val register_localized_collators by sqliteBindings.functionMember()
    override val register_android_functions by sqliteBindings.functionMember()

    override val memoryExports: GraalvmSqliteDynamicDynamicMemoryExports =
        GraalvmSqliteDynamicDynamicMemoryExports(sqliteBindings)
}

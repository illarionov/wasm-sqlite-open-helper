/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory.exports

import at.released.wasm.sqlite.open.helper.chicory.ext.functionMember
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import com.dylibso.chicory.runtime.Instance

@Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")
internal class ChicorySqliteExports(instance: Instance) : SqliteExports {
    override val sqlite3_db_status by instance.functionMember()
    override val sqlite3_initialize by instance.functionMember()

    override val sqlite3_prepare_v2 by instance.functionMember()
    override val sqlite3_step by instance.functionMember()
    override val sqlite3_column_int64 by instance.functionMember()
    override val sqlite3_finalize by instance.functionMember()
    override val sqlite3_reset by instance.functionMember()
    override val sqlite3_clear_bindings by instance.functionMember()
    override val sqlite3_column_count by instance.functionMember()
    override val sqlite3_column_bytes by instance.functionMember()
    override val sqlite3_column_double by instance.functionMember()
    override val sqlite3_column_text by instance.functionMember()
    override val sqlite3_column_type by instance.functionMember()
    override val sqlite3_column_name by instance.functionMember()
    override val sqlite3_bind_blob by instance.functionMember()
    override val sqlite3_bind_double by instance.functionMember()
    override val sqlite3_bind_int64 by instance.functionMember()
    override val sqlite3_bind_null by instance.functionMember()
    override val sqlite3_bind_text by instance.functionMember()
    override val sqlite3_bind_parameter_count by instance.functionMember()
    override val sqlite3_stmt_readonly by instance.functionMember()
    override val sqlite3_expanded_sql by instance.functionMember()
    override val sqlite3_errmsg by instance.functionMember()
    override val sqlite3_libversion by instance.functionMember()
    override val sqlite3_libversion_number by instance.functionMember()
    override val sqlite3_last_insert_rowid by instance.functionMember()
    override val sqlite3_changes by instance.functionMember()
    override val sqlite3_close_v2 by instance.functionMember()
    override val sqlite3_progress_handler by instance.functionMember()
    override val sqlite3_soft_heap_limit64 by instance.functionMember()
    override val sqlite3_busy_timeout by instance.functionMember()
    override val sqlite3_trace_v2 by instance.functionMember()
    override val sqlite3_errcode by instance.functionMember()
    override val sqlite3_extended_errcode by instance.functionMember()
    override val sqlite3_open by instance.functionMember()
    override val sqlite3_open_v2 by instance.functionMember()
    override val sqlite3_db_readonly by instance.functionMember()

    override val sqlite3_sourceid by instance.functionMember()

    override val sqlite3__wasm_enum_json by instance.functionMember()
    override val sqlite3__wasm_config_i by instance.functionMember()
    override val sqlite3__wasm_config_ii by instance.functionMember()
    override val sqlite3__wasm_config_j by instance.functionMember()
    override val sqlite3__wasm_db_config_ip by instance.functionMember()
    override val sqlite3__wasm_db_config_pii by instance.functionMember()
    override val sqlite3__wasm_db_config_s by instance.functionMember()

    override val register_localized_collators by instance.functionMember()
    override val register_android_functions by instance.functionMember()

    override val memoryExports: ChicorySqliteDynamicMemoryExports = ChicorySqliteDynamicMemoryExports(instance)
}

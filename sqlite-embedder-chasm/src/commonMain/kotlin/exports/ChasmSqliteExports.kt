/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")

package at.released.wasm.sqlite.open.helper.chasm.exports

import at.released.wasm.sqlite.open.helper.chasm.ext.functionMember
import at.released.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import at.released.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import at.released.weh.wasm.core.WasmFunctionBinding

internal class ChasmSqliteExports(instance: ChasmInstance) : SqliteExports {
    override val sqlite3_db_status: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_initialize: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_prepare_v2: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_step: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_column_int64: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_finalize: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_reset: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_clear_bindings: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_column_count: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_column_bytes: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_column_double: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_column_text: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_column_type: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_column_name: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_bind_blob: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_bind_double: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_bind_int64: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_bind_null: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_bind_text: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_bind_parameter_count: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_stmt_readonly: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_expanded_sql: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_errmsg: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_libversion: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_libversion_number: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_last_insert_rowid: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_changes: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_close_v2: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_progress_handler: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_soft_heap_limit64: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_busy_timeout: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_trace_v2: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_errcode: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_extended_errcode: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_open: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_open_v2: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_db_readonly: WasmFunctionBinding by instance.functionMember()
    override val sqlite3_sourceid: WasmFunctionBinding by instance.functionMember()
    override val sqlite3__wasm_enum_json: WasmFunctionBinding by instance.functionMember()
    override val sqlite3__wasm_config_i: WasmFunctionBinding by instance.functionMember()
    override val sqlite3__wasm_config_ii: WasmFunctionBinding by instance.functionMember()
    override val sqlite3__wasm_config_j: WasmFunctionBinding by instance.functionMember()
    override val sqlite3__wasm_db_config_ip: WasmFunctionBinding by instance.functionMember()
    override val sqlite3__wasm_db_config_pii: WasmFunctionBinding by instance.functionMember()
    override val sqlite3__wasm_db_config_s: WasmFunctionBinding by instance.functionMember()
    override val register_localized_collators: WasmFunctionBinding by instance.functionMember()
    override val register_android_functions: WasmFunctionBinding by instance.functionMember()
    override val memoryExports: ChasmSqliteDynamicMemoryExports = ChasmSqliteDynamicMemoryExports(instance)
}

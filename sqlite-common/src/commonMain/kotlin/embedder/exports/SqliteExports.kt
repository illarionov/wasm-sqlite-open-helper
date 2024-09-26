/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming")

package ru.pixnews.wasm.sqlite.open.helper.embedder.exports

import at.released.weh.wasm.core.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public interface SqliteExports {
    public val sqlite3_db_status: WasmFunctionBinding
    public val sqlite3_initialize: WasmFunctionBinding
    public val sqlite3_prepare_v2: WasmFunctionBinding
    public val sqlite3_step: WasmFunctionBinding
    public val sqlite3_column_int64: WasmFunctionBinding
    public val sqlite3_finalize: WasmFunctionBinding
    public val sqlite3_reset: WasmFunctionBinding
    public val sqlite3_clear_bindings: WasmFunctionBinding
    public val sqlite3_column_count: WasmFunctionBinding
    public val sqlite3_column_bytes: WasmFunctionBinding
    public val sqlite3_column_double: WasmFunctionBinding
    public val sqlite3_column_text: WasmFunctionBinding
    public val sqlite3_column_type: WasmFunctionBinding
    public val sqlite3_column_name: WasmFunctionBinding
    public val sqlite3_bind_blob: WasmFunctionBinding
    public val sqlite3_bind_double: WasmFunctionBinding
    public val sqlite3_bind_int64: WasmFunctionBinding
    public val sqlite3_bind_null: WasmFunctionBinding
    public val sqlite3_bind_text: WasmFunctionBinding
    public val sqlite3_bind_parameter_count: WasmFunctionBinding
    public val sqlite3_stmt_readonly: WasmFunctionBinding
    public val sqlite3_expanded_sql: WasmFunctionBinding
    public val sqlite3_errmsg: WasmFunctionBinding
    public val sqlite3_libversion: WasmFunctionBinding
    public val sqlite3_libversion_number: WasmFunctionBinding
    public val sqlite3_last_insert_rowid: WasmFunctionBinding
    public val sqlite3_changes: WasmFunctionBinding
    public val sqlite3_close_v2: WasmFunctionBinding
    public val sqlite3_progress_handler: WasmFunctionBinding
    public val sqlite3_soft_heap_limit64: WasmFunctionBinding
    public val sqlite3_busy_timeout: WasmFunctionBinding
    public val sqlite3_trace_v2: WasmFunctionBinding
    public val sqlite3_errcode: WasmFunctionBinding
    public val sqlite3_extended_errcode: WasmFunctionBinding
    public val sqlite3_open: WasmFunctionBinding
    public val sqlite3_open_v2: WasmFunctionBinding
    public val sqlite3_db_readonly: WasmFunctionBinding
    public val sqlite3_sourceid: WasmFunctionBinding
    public val sqlite3__wasm_enum_json: WasmFunctionBinding
    public val sqlite3__wasm_config_i: WasmFunctionBinding
    public val sqlite3__wasm_config_ii: WasmFunctionBinding
    public val sqlite3__wasm_config_j: WasmFunctionBinding
    public val sqlite3__wasm_db_config_ip: WasmFunctionBinding
    public val sqlite3__wasm_db_config_pii: WasmFunctionBinding
    public val sqlite3__wasm_db_config_s: WasmFunctionBinding
    public val register_localized_collators: WasmFunctionBinding
    public val register_android_functions: WasmFunctionBinding
    public val memoryExports: SqliteDynamicMemoryExports
}

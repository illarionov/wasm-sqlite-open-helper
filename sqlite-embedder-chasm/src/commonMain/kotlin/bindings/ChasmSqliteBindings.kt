/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")

package ru.pixnews.wasm.sqlite.open.helper.chasm.bindings

import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.member
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.ChasmInstanceBuilder.ChasmInstance
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmFunctionBinding

internal class ChasmSqliteBindings(
    instance: ChasmInstance,
) : SqliteBindings {
    private val memory = instance.memory
    val _initialize: WasmFunctionBinding by instance.member()
    val __errno_location: WasmFunctionBinding by instance.member()
    val __wasm_call_ctors by instance.member()

    override val sqlite3_db_status: WasmFunctionBinding by instance.member()
    override val sqlite3_initialize: WasmFunctionBinding by instance.member()
    override val sqlite3_prepare_v2: WasmFunctionBinding by instance.member()
    override val sqlite3_step: WasmFunctionBinding by instance.member()
    override val sqlite3_column_int64: WasmFunctionBinding by instance.member()
    override val sqlite3_finalize: WasmFunctionBinding by instance.member()
    override val sqlite3_reset: WasmFunctionBinding by instance.member()
    override val sqlite3_clear_bindings: WasmFunctionBinding by instance.member()
    override val sqlite3_column_count: WasmFunctionBinding by instance.member()
    override val sqlite3_column_bytes: WasmFunctionBinding by instance.member()
    override val sqlite3_column_double: WasmFunctionBinding by instance.member()
    override val sqlite3_column_text: WasmFunctionBinding by instance.member()
    override val sqlite3_column_type: WasmFunctionBinding by instance.member()
    override val sqlite3_column_name: WasmFunctionBinding by instance.member()
    override val sqlite3_bind_blob: WasmFunctionBinding by instance.member()
    override val sqlite3_bind_double: WasmFunctionBinding by instance.member()
    override val sqlite3_bind_int64: WasmFunctionBinding by instance.member()
    override val sqlite3_bind_null: WasmFunctionBinding by instance.member()
    override val sqlite3_bind_text: WasmFunctionBinding by instance.member()
    override val sqlite3_bind_parameter_count: WasmFunctionBinding by instance.member()
    override val sqlite3_stmt_readonly: WasmFunctionBinding by instance.member()
    override val sqlite3_expanded_sql: WasmFunctionBinding by instance.member()
    override val sqlite3_errmsg: WasmFunctionBinding by instance.member()
    override val sqlite3_libversion: WasmFunctionBinding by instance.member()
    override val sqlite3_libversion_number: WasmFunctionBinding by instance.member()
    override val sqlite3_last_insert_rowid: WasmFunctionBinding by instance.member()
    override val sqlite3_changes: WasmFunctionBinding by instance.member()
    override val sqlite3_close_v2: WasmFunctionBinding by instance.member()
    override val sqlite3_progress_handler: WasmFunctionBinding by instance.member()
    override val sqlite3_soft_heap_limit64: WasmFunctionBinding by instance.member()
    override val sqlite3_busy_timeout: WasmFunctionBinding by instance.member()
    override val sqlite3_trace_v2: WasmFunctionBinding by instance.member()
    override val sqlite3_errcode: WasmFunctionBinding by instance.member()
    override val sqlite3_extended_errcode: WasmFunctionBinding by instance.member()
    override val sqlite3_open: WasmFunctionBinding by instance.member()
    override val sqlite3_open_v2: WasmFunctionBinding by instance.member()
    override val sqlite3_db_readonly: WasmFunctionBinding by instance.member()
    override val sqlite3_sourceid: WasmFunctionBinding by instance.member()
    override val sqlite3__wasm_enum_json: WasmFunctionBinding by instance.member()
    override val sqlite3__wasm_config_i: WasmFunctionBinding by instance.member()
    override val sqlite3__wasm_config_ii: WasmFunctionBinding by instance.member()
    override val sqlite3__wasm_config_j: WasmFunctionBinding by instance.member()
    override val sqlite3__wasm_db_config_ip: WasmFunctionBinding by instance.member()
    override val sqlite3__wasm_db_config_pii: WasmFunctionBinding by instance.member()
    override val sqlite3__wasm_db_config_s: WasmFunctionBinding by instance.member()
    override val register_localized_collators: WasmFunctionBinding by instance.member()
    override val register_android_functions: WasmFunctionBinding by instance.member()

    override val memoryBindings: ChasmSqliteMemoryBindings = ChasmSqliteMemoryBindings(instance)

    override fun init() {
        __wasm_call_ctors.executeVoid()
        memoryBindings.init(memory)
    }
}

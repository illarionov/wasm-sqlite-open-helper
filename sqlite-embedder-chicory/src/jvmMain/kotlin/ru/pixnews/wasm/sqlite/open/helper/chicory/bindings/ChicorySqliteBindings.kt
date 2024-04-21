/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.bindings

import com.dylibso.chicory.runtime.Instance
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.member
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.embedder.bindings.SqliteMemoryBindings
import ru.pixnews.wasm.sqlite.open.helper.host.WasmFunctionBinding
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory

@Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")
internal class ChicorySqliteBindings(
    val memory: Memory,
    private val instance: Instance,
) : SqliteBindings {
    val _initialize: WasmFunctionBinding by instance.member()
    val __errno_location: WasmFunctionBinding by instance.member()
    val __wasm_call_ctors by instance.member()

    override val sqlite3_db_status by instance.member()
    override val sqlite3_initialize by instance.member()

    override val sqlite3_prepare_v2 by instance.member()
    override val sqlite3_step by instance.member()
    override val sqlite3_column_int64 by instance.member()
    override val sqlite3_finalize by instance.member()
    override val sqlite3_reset by instance.member()
    override val sqlite3_clear_bindings by instance.member()
    override val sqlite3_column_count by instance.member()
    override val sqlite3_column_bytes by instance.member()
    override val sqlite3_column_double by instance.member()
    override val sqlite3_column_text by instance.member()
    override val sqlite3_column_type by instance.member()
    override val sqlite3_column_name by instance.member()
    override val sqlite3_bind_blob by instance.member()
    override val sqlite3_bind_double by instance.member()
    override val sqlite3_bind_int64 by instance.member()
    override val sqlite3_bind_null by instance.member()
    override val sqlite3_bind_text by instance.member()
    override val sqlite3_bind_parameter_count by instance.member()
    override val sqlite3_stmt_readonly by instance.member()
    override val sqlite3_expanded_sql by instance.member()
    override val sqlite3_errmsg by instance.member()
    override val sqlite3_exec by instance.member()
    override val sqlite3_libversion by instance.member()
    override val sqlite3_libversion_number by instance.member()
    override val sqlite3_last_insert_rowid by instance.member()
    override val sqlite3_changes by instance.member()
    override val sqlite3_close_v2 by instance.member()
    override val sqlite3_progress_handler by instance.member()
    override val sqlite3_soft_heap_limit64 by instance.member()
    override val sqlite3_busy_timeout by instance.member()
    override val sqlite3_trace_v2 by instance.member()
    override val sqlite3_errcode by instance.member()
    override val sqlite3_extended_errcode by instance.member()
    override val sqlite3_open by instance.member()
    override val sqlite3_open_v2 by instance.member()
    override val sqlite3_create_collation_v2 by instance.member()
    override val sqlite3_db_readonly by instance.member()

    override val sqlite3_sourceid by instance.member()

    override val sqlite3__wasm_enum_json by instance.member()
    override val sqlite3__wasm_config_i by instance.member()
    override val sqlite3__wasm_config_ii by instance.member()
    override val sqlite3__wasm_config_j by instance.member()
    override val sqlite3__wasm_db_config_ip by instance.member()
    override val sqlite3__wasm_db_config_pii by instance.member()
    override val sqlite3__wasm_db_config_s by instance.member()

    override val register_localized_collators by instance.member()
    override val register_android_functions by instance.member()

    private val _memoryBindings = ChicorySqliteMemoryBindings(instance)
    override val memoryBindings: SqliteMemoryBindings = _memoryBindings

    init {
        init()
    }

    override fun init() {
        __wasm_call_ctors.executeVoid()
        _memoryBindings.init(memory)
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.wasm.builder.sqlite

public object SqliteExportedFunctions {
    public val openHelper: List<String> = listOf(
        "_sqlite3_initialize",
        "_sqlite3_bind_blob",
        "_sqlite3_bind_double",
        "_sqlite3_bind_int64",
        "_sqlite3_bind_parameter_count",
        "_sqlite3_bind_text",
        "_sqlite3_busy_timeout",
        "_sqlite3_changes",
        "_sqlite3_clear_bindings",
        "_sqlite3_close_v2",
        "_sqlite3_column_bytes",
        "_sqlite3_column_count",
        "_sqlite3_column_double",
        "_sqlite3_column_int64",
        "_sqlite3_column_name",
        "_sqlite3_column_text",
        "_sqlite3_column_type",
        "_sqlite3_create_collation_v2",
        "_sqlite3_db_readonly",
        "_sqlite3_db_status",
        "_sqlite3_errcode",
        "_sqlite3_errmsg",
        "_sqlite3_exec", // optional
        "_sqlite3_expanded_sql",
        "_sqlite3_extended_errcode",
        "_sqlite3_finalize",
        "_sqlite3_free",
        "_sqlite3_last_insert_rowid",
        "_sqlite3_libversion", // optional
        "_sqlite3_libversion_number", // optional
        "_sqlite3_malloc",
        "_sqlite3_open",
        "_sqlite3_open_v2",
        "_sqlite3_prepare_v2",
        "_sqlite3_progress_handler",
        "_sqlite3_reset",
        "_sqlite3_sourceid",
        "_sqlite3_step",
        "_sqlite3_stmt_readonly",
        "_sqlite3_trace_v2",
        "_sqlite3__wasm_enum_json", // optional
    )
    public val defaultWasm: List<String> = listOf(
        "_free",
        "_malloc",
        "_realloc",
        "_sqlite3_aggregate_context",
        "_sqlite3_auto_extension",
        "_sqlite3_bind_blob",
        "_sqlite3_bind_double",
        "_sqlite3_bind_int",
        "_sqlite3_bind_int64",
        "_sqlite3_bind_null",
        "_sqlite3_bind_parameter_count",
        "_sqlite3_bind_parameter_index",
        "_sqlite3_bind_pointer",
        "_sqlite3_bind_text",
        "_sqlite3_busy_handler",
        "_sqlite3_busy_timeout",
        "_sqlite3_cancel_auto_extension",
        "_sqlite3_changes",
        "_sqlite3_changes64",
        "_sqlite3_clear_bindings",
        "_sqlite3_close_v2",
        "_sqlite3_collation_needed",
        "_sqlite3_column_blob",
        "_sqlite3_column_bytes",
        "_sqlite3_column_count",
        "_sqlite3_column_count",
        "_sqlite3_column_double",
        "_sqlite3_column_int",
        "_sqlite3_column_int64",
        "_sqlite3_column_name",
        "_sqlite3_column_text",
        "_sqlite3_column_type",
        "_sqlite3_column_value",
        "_sqlite3_commit_hook",
        "_sqlite3_compileoption_get",
        "_sqlite3_compileoption_used",
        "_sqlite3_complete",
        "_sqlite3_context_db_handle",
        "_sqlite3_create_collation",
        "_sqlite3_create_collation_v2",
        "_sqlite3_create_function",
        "_sqlite3_create_function_v2",
        "_sqlite3_create_module",
        "_sqlite3_create_module_v2",
        "_sqlite3_create_window_function",
        "_sqlite3_data_count",
        "_sqlite3_db_filename",
        "_sqlite3_db_handle",
        "_sqlite3_db_name",
        "_sqlite3_db_readonly",
        "_sqlite3_db_status",
        "_sqlite3_declare_vtab",
        "_sqlite3_deserialize",
        "_sqlite3_drop_modules",
        "_sqlite3_errcode",
        "_sqlite3_errmsg",
        "_sqlite3_error_offset",
        "_sqlite3_errstr",
        "_sqlite3_exec",
        "_sqlite3_expanded_sql",
        "_sqlite3_extended_errcode",
        "_sqlite3_extended_result_codes",
        "_sqlite3_file_control",
        "_sqlite3_finalize",
        "_sqlite3_free",
        "_sqlite3_get_autocommit",
        "_sqlite3_get_auxdata",
        "_sqlite3_initialize",
        "_sqlite3_keyword_check",
        "_sqlite3_keyword_count",
        "_sqlite3_keyword_name",
        "_sqlite3_last_insert_rowid",
        "_sqlite3_libversion",
        "_sqlite3_libversion_number",
        "_sqlite3_limit",
        "_sqlite3_malloc",
        "_sqlite3_malloc64",
        "_sqlite3_msize",
        "_sqlite3_open",
        "_sqlite3_open_v2",
        "_sqlite3_overload_function",
        "_sqlite3_prepare_v2",
        "_sqlite3_prepare_v3",
        "_sqlite3_preupdate_blobwrite",
        "_sqlite3_preupdate_count",
        "_sqlite3_preupdate_depth",
        "_sqlite3_preupdate_hook",
        "_sqlite3_preupdate_new",
        "_sqlite3_preupdate_old",
        "_sqlite3_progress_handler",
        "_sqlite3_randomness",
        "_sqlite3_realloc",
        "_sqlite3_realloc64",
        "_sqlite3_reset",
        "_sqlite3_reset_auto_extension",
        "_sqlite3_result_blob",
        "_sqlite3_result_double",
        "_sqlite3_result_error",
        "_sqlite3_result_error_code",
        "_sqlite3_result_error_nomem",
        "_sqlite3_result_error_toobig",
        "_sqlite3_result_int",
        "_sqlite3_result_int64",
        "_sqlite3_result_null",
        "_sqlite3_result_pointer",
        "_sqlite3_result_subtype",
        "_sqlite3_result_text",
        "_sqlite3_result_zeroblob",
        "_sqlite3_result_zeroblob64",
        "_sqlite3_rollback_hook",
        "_sqlite3_serialize",
        "_sqlite3_set_authorizer",
        "_sqlite3_set_auxdata",
        "_sqlite3_set_last_insert_rowid",
        "_sqlite3_shutdown",
        "_sqlite3_sourceid",
        "_sqlite3_sql",
        "_sqlite3_status",
        "_sqlite3_status64",
        "_sqlite3_step",
        "_sqlite3_stmt_isexplain",
        "_sqlite3_stmt_readonly",
        "_sqlite3_stmt_status",
        "_sqlite3_strglob",
        "_sqlite3_stricmp",
        "_sqlite3_strlike",
        "_sqlite3_strnicmp",
        "_sqlite3_table_column_metadata",
        "_sqlite3_total_changes",
        "_sqlite3_total_changes64",
        "_sqlite3_trace_v2",
        "_sqlite3_txn_state",
        "_sqlite3_update_hook",
        "_sqlite3_uri_boolean",
        "_sqlite3_uri_int64",
        "_sqlite3_uri_key",
        "_sqlite3_uri_parameter",
        "_sqlite3_user_data",
        "_sqlite3_value_blob",
        "_sqlite3_value_bytes",
        "_sqlite3_value_double",
        "_sqlite3_value_dup",
        "_sqlite3_value_free",
        "_sqlite3_value_frombind",
        "_sqlite3_value_int",
        "_sqlite3_value_int64",
        "_sqlite3_value_nochange",
        "_sqlite3_value_numeric_type",
        "_sqlite3_value_pointer",
        "_sqlite3_value_subtype",
        "_sqlite3_value_text",
        "_sqlite3_value_type",
        "_sqlite3_vfs_find",
        "_sqlite3_vfs_register",
        "_sqlite3_vfs_unregister",
        "_sqlite3_vtab_collation",
        "_sqlite3_vtab_distinct",
        "_sqlite3_vtab_in",
        "_sqlite3_vtab_in_first",
        "_sqlite3_vtab_in_next",
        "_sqlite3_vtab_nochange",
        "_sqlite3_vtab_on_conflict",
        "_sqlite3_vtab_rhs_value",
        "_sqlite3changegroup_add",
        "_sqlite3changegroup_add_strm",
        "_sqlite3changegroup_delete",
        "_sqlite3changegroup_new",
        "_sqlite3changegroup_output",
        "_sqlite3changegroup_output_strm",
        "_sqlite3changeset_apply",
        "_sqlite3changeset_apply_strm",
        "_sqlite3changeset_apply_v2",
        "_sqlite3changeset_apply_v2_strm",
        "_sqlite3changeset_concat",
        "_sqlite3changeset_concat_strm",
        "_sqlite3changeset_conflict",
        "_sqlite3changeset_finalize",
        "_sqlite3changeset_fk_conflicts",
        "_sqlite3changeset_invert",
        "_sqlite3changeset_invert_strm",
        "_sqlite3changeset_new",
        "_sqlite3changeset_next",
        "_sqlite3changeset_old",
        "_sqlite3changeset_op",
        "_sqlite3changeset_pk",
        "_sqlite3changeset_start",
        "_sqlite3changeset_start_strm",
        "_sqlite3changeset_start_v2",
        "_sqlite3changeset_start_v2_strm",
        "_sqlite3session_attach",
        "_sqlite3session_changeset",
        "_sqlite3session_changeset_size",
        "_sqlite3session_changeset_strm",
        "_sqlite3session_config",
        "_sqlite3session_create",
        "_sqlite3session_delete",
        "_sqlite3session_diff",
        "_sqlite3session_enable",
        "_sqlite3session_indirect",
        "_sqlite3session_isempty",
        "_sqlite3session_memory_used",
        "_sqlite3session_object_config",
        "_sqlite3session_patchset",
        "_sqlite3session_patchset_strm",
        "_sqlite3session_table_filter",
    )
}

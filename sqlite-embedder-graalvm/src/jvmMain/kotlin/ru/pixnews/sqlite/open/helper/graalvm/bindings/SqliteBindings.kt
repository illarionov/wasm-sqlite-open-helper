/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.bindings

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import ru.pixnews.sqlite.open.helper.graalvm.host.memory.GraalHostMemoryImpl
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteErrno
import ru.pixnews.sqlite.open.helper.sqlite.common.api.SqliteException

@Suppress("VariableNaming", "BLANK_LINE_BETWEEN_PROPERTIES")
internal class SqliteBindings(
    val context: Context,
    envBindings: Value = context.getBindings("wasm").getMember("env"),
    mainBindings: Value = context.getBindings("wasm").getMember("main"),
) {
    val _initialize: Value? = mainBindings.getMember("_initialize") // 34
    val __errno_location = mainBindings.getMember("__errno_location") // 2644
    val __wasm_call_ctors: Value? = mainBindings.getMember("__wasm_call_ctors") // 34

    val sqlite3_status64 = mainBindings.getMember("sqlite3_status64") // 35
    val sqlite3_status = mainBindings.getMember("sqlite3_status") // 38
    val sqlite3_db_status = mainBindings.getMember("sqlite3_db_status") // 39
    val sqlite3_msize = mainBindings.getMember("sqlite3_msize") // 48
    val sqlite3_vfs_find = mainBindings.getMember("sqlite3_vfs_find") // 58
    val sqlite3_initialize = mainBindings.getMember("sqlite3_initialize") // 59

    val sqlite3_vfs_register = mainBindings.getMember("sqlite3_vfs_register") // 66
    val sqlite3_vfs_unregister = mainBindings.getMember("sqlite3_vfs_unregister") // 69
    val sqlite3_value_text = mainBindings.getMember("sqlite3_value_text") // 95
    val sqlite3_randomness = mainBindings.getMember("sqlite3_randomness") // 107
    val sqlite3_stricmp = mainBindings.getMember("sqlite3_stricmp") // 108
    val sqlite3_strnicmp = mainBindings.getMember("sqlite3_strnicmp") // 110
    val sqlite3_uri_parameter = mainBindings.getMember("sqlite3_uri_parameter") // 114
    val sqlite3_uri_boolean = mainBindings.getMember("sqlite3_uri_boolean") // 116
    val sqlite3_serialize = mainBindings.getMember("sqlite3_serialize") // 132
    val sqlite3_prepare_v2 = mainBindings.getMember("sqlite3_prepare_v2") // 135
    val sqlite3_step = mainBindings.getMember("sqlite3_step") // 136
    val sqlite3_column_int64 = mainBindings.getMember("sqlite3_column_int64") // 137
    val sqlite3_column_int = mainBindings.getMember("sqlite3_column_int") // 138
    val sqlite3_finalize = mainBindings.getMember("sqlite3_finalize") // 140
    val sqlite3_file_control = mainBindings.getMember("sqlite3_file_control") // 141
    val sqlite3_reset = mainBindings.getMember("sqlite3_reset") // 146
    val sqlite3_deserialize = mainBindings.getMember("sqlite3_deserialize") // 165
    val sqlite3_clear_bindings = mainBindings.getMember("sqlite3_clear_bindings") // 240
    val sqlite3_value_blob = mainBindings.getMember("sqlite3_value_blob") // 243
    val sqlite3_value_bytes = mainBindings.getMember("sqlite3_value_bytes") // 247
    val sqlite3_value_double = mainBindings.getMember("sqlite3_value_double") // 251
    val sqlite3_value_int = mainBindings.getMember("sqlite3_value_int") // 253
    val sqlite3_value_int64 = mainBindings.getMember("sqlite3_value_int64") // 255
    val sqlite3_value_subtype = mainBindings.getMember("sqlite3_value_subtype") // 256
    val sqlite3_value_pointer = mainBindings.getMember("sqlite3_value_pointer") // 257
    val sqlite3_value_type = mainBindings.getMember("sqlite3_value_type") // 259
    val sqlite3_value_nochange = mainBindings.getMember("sqlite3_value_nochange") // 260
    val sqlite3_value_frombind = mainBindings.getMember("sqlite3_value_frombind") // 261
    val sqlite3_value_dup = mainBindings.getMember("sqlite3_value_dup") // 262
    val sqlite3_value_free = mainBindings.getMember("sqlite3_value_free") // 265
    val sqlite3_result_blob = mainBindings.getMember("sqlite3_result_blob") // 266
    val sqlite3_result_error_toobig = mainBindings.getMember("sqlite3_result_error_toobig") // 269
    val sqlite3_result_error_nomem = mainBindings.getMember("sqlite3_result_error_nomem") // 270
    val sqlite3_result_double = mainBindings.getMember("sqlite3_result_double") // 273
    val sqlite3_result_error = mainBindings.getMember("sqlite3_result_error") // 276
    val sqlite3_result_int = mainBindings.getMember("sqlite3_result_int") // 279
    val sqlite3_result_int64 = mainBindings.getMember("sqlite3_result_int64") // 281
    val sqlite3_result_null = mainBindings.getMember("sqlite3_result_null") // 282
    val sqlite3_result_pointer = mainBindings.getMember("sqlite3_result_pointer") // 284
    val sqlite3_result_subtype = mainBindings.getMember("sqlite3_result_subtype") // 287
    val sqlite3_result_text = mainBindings.getMember("sqlite3_result_text") // 288
    val sqlite3_result_zeroblob = mainBindings.getMember("sqlite3_result_zeroblob") // 294
    val sqlite3_result_zeroblob64 = mainBindings.getMember("sqlite3_result_zeroblob64") // 295
    val sqlite3_result_error_code = mainBindings.getMember("sqlite3_result_error_code") // 297
    val sqlite3_user_data = mainBindings.getMember("sqlite3_user_data") // 302
    val sqlite3_context_db_handle = mainBindings.getMember("sqlite3_context_db_handle") // 303
    val sqlite3_vtab_nochange = mainBindings.getMember("sqlite3_vtab_nochange") // 304
    val sqlite3_vtab_in_first = mainBindings.getMember("sqlite3_vtab_in_first") // 305
    val sqlite3_vtab_in_next = mainBindings.getMember("sqlite3_vtab_in_next") // 314
    val sqlite3_aggregate_context = mainBindings.getMember("sqlite3_aggregate_context") // 315
    val sqlite3_get_auxdata = mainBindings.getMember("sqlite3_get_auxdata") // 317
    val sqlite3_set_auxdata = mainBindings.getMember("sqlite3_set_auxdata") // 318
    val sqlite3_column_count = mainBindings.getMember("sqlite3_column_count") // 320
    val sqlite3_data_count = mainBindings.getMember("sqlite3_data_count") // 321
    val sqlite3_column_blob = mainBindings.getMember("sqlite3_column_blob") // 322
    val sqlite3_column_bytes = mainBindings.getMember("sqlite3_column_bytes") // 323
    val sqlite3_column_double = mainBindings.getMember("sqlite3_column_double") // 324
    val sqlite3_column_text = mainBindings.getMember("sqlite3_column_text") // 325
    val sqlite3_column_value = mainBindings.getMember("sqlite3_column_value") // 326
    val sqlite3_column_type = mainBindings.getMember("sqlite3_column_type") // 327
    val sqlite3_column_name = mainBindings.getMember("sqlite3_column_name") // 328
    val sqlite3_bind_blob = mainBindings.getMember("sqlite3_bind_blob") // 330
    val sqlite3_bind_double = mainBindings.getMember("sqlite3_bind_double") // 333
    val sqlite3_bind_int = mainBindings.getMember("sqlite3_bind_int") // 334
    val sqlite3_bind_int64 = mainBindings.getMember("sqlite3_bind_int64") // 335
    val sqlite3_bind_null = mainBindings.getMember("sqlite3_bind_null") // 336
    val sqlite3_bind_pointer = mainBindings.getMember("sqlite3_bind_pointer") // 337
    val sqlite3_bind_text = mainBindings.getMember("sqlite3_bind_text") // 338
    val sqlite3_bind_parameter_count = mainBindings.getMember("sqlite3_bind_parameter_count") // 341
    val sqlite3_bind_parameter_index = mainBindings.getMember("sqlite3_bind_parameter_index") // 343
    val sqlite3_db_handle = mainBindings.getMember("sqlite3_db_handle") // 346
    val sqlite3_stmt_readonly = mainBindings.getMember("sqlite3_stmt_readonly") // 347
    val sqlite3_stmt_isexplain = mainBindings.getMember("sqlite3_stmt_isexplain") // 348
    val sqlite3_stmt_status = mainBindings.getMember("sqlite3_stmt_status") // 350
    val sqlite3_sql = mainBindings.getMember("sqlite3_sql") // 351
    val sqlite3_expanded_sql = mainBindings.getMember("sqlite3_expanded_sql") // 352
    val sqlite3_preupdate_old = mainBindings.getMember("sqlite3_preupdate_old") // 355
    val sqlite3_preupdate_count = mainBindings.getMember("sqlite3_preupdate_count") // 365
    val sqlite3_preupdate_depth = mainBindings.getMember("sqlite3_preupdate_depth") // 366
    val sqlite3_preupdate_blobwrite = mainBindings.getMember("sqlite3_preupdate_blobwrite") // 367
    val sqlite3_preupdate_new = mainBindings.getMember("sqlite3_preupdate_new") // 368
    val sqlite3_value_numeric_type = mainBindings.getMember("sqlite3_value_numeric_type") // 369
    val sqlite3_errmsg = mainBindings.getMember("sqlite3_errmsg") // 396
    val sqlite3_set_authorizer = mainBindings.getMember("sqlite3_set_authorizer") // 409
    val sqlite3_strglob = mainBindings.getMember("sqlite3_strglob") // 411
    val sqlite3_strlike = mainBindings.getMember("sqlite3_strlike") // 414
    val sqlite3_exec = mainBindings.getMember("sqlite3_exec") // 415
    val sqlite3_auto_extension = mainBindings.getMember("sqlite3_auto_extension") // 416
    val sqlite3_cancel_auto_extension = mainBindings.getMember("sqlite3_cancel_auto_extension") // 417
    val sqlite3_reset_auto_extension = mainBindings.getMember("sqlite3_reset_auto_extension") // 418
    val sqlite3_prepare_v3 = mainBindings.getMember("sqlite3_prepare_v3") // 422
    val sqlite3_create_module = mainBindings.getMember("sqlite3_create_module") // 423
    val sqlite3_create_module_v2 = mainBindings.getMember("sqlite3_create_module_v2") // 425
    val sqlite3_drop_modules = mainBindings.getMember("sqlite3_drop_modules") // 426
    val sqlite3_declare_vtab = mainBindings.getMember("sqlite3_declare_vtab") // 427
    val sqlite3_vtab_on_conflict = mainBindings.getMember("sqlite3_vtab_on_conflict") // 436
    val sqlite3_vtab_collation = mainBindings.getMember("sqlite3_vtab_collation") // 438
    val sqlite3_vtab_in = mainBindings.getMember("sqlite3_vtab_in") // 441
    val sqlite3_vtab_rhs_value = mainBindings.getMember("sqlite3_vtab_rhs_value") // 442
    val sqlite3_vtab_distinct = mainBindings.getMember("sqlite3_vtab_distinct") // 445
    val sqlite3_keyword_name = mainBindings.getMember("sqlite3_keyword_name") // 446
    val sqlite3_keyword_count = mainBindings.getMember("sqlite3_keyword_count") // 447
    val sqlite3_keyword_check = mainBindings.getMember("sqlite3_keyword_check") // 448
    val sqlite3_complete = mainBindings.getMember("sqlite3_complete") // 451
    val sqlite3_libversion = mainBindings.getMember("sqlite3_libversion") // 452
    val sqlite3_libversion_number = mainBindings.getMember("sqlite3_libversion_number") // 453
    val sqlite3_shutdown = mainBindings.getMember("sqlite3_shutdown") // 454
    val sqlite3_last_insert_rowid = mainBindings.getMember("sqlite3_last_insert_rowid") // 460
    val sqlite3_set_last_insert_rowid = mainBindings.getMember("sqlite3_set_last_insert_rowid") // 461
    val sqlite3_changes64 = mainBindings.getMember("sqlite3_changes64") // 462
    val sqlite3_changes = mainBindings.getMember("sqlite3_changes") // 463
    val sqlite3_total_changes64 = mainBindings.getMember("sqlite3_total_changes64") // 464
    val sqlite3_total_changes = mainBindings.getMember("sqlite3_total_changes") // 465
    val sqlite3_txn_state = mainBindings.getMember("sqlite3_txn_state") // 466
    val sqlite3_close_v2 = mainBindings.getMember("sqlite3_close_v2") // 471
    val sqlite3_busy_handler = mainBindings.getMember("sqlite3_busy_handler") // 472
    val sqlite3_progress_handler = mainBindings.getMember("sqlite3_progress_handler") // 473
    val sqlite3_busy_timeout = mainBindings.getMember("sqlite3_busy_timeout") // 474
    val sqlite3_create_function = mainBindings.getMember("sqlite3_create_function") // 476
    val sqlite3_create_function_v2 = mainBindings.getMember("sqlite3_create_function_v2") // 479
    val sqlite3_create_window_function = mainBindings.getMember("sqlite3_create_window_function") // 480
    val sqlite3_overload_function = mainBindings.getMember("sqlite3_overload_function") // 481
    val sqlite3_trace_v2 = mainBindings.getMember("sqlite3_trace_v2") // 487
    val sqlite3_commit_hook = mainBindings.getMember("sqlite3_commit_hook") // 488
    val sqlite3_update_hook = mainBindings.getMember("sqlite3_update_hook") // 489
    val sqlite3_rollback_hook = mainBindings.getMember("sqlite3_rollback_hook") // 490
    val sqlite3_preupdate_hook = mainBindings.getMember("sqlite3_preupdate_hook") // 491
    val sqlite3_error_offset = mainBindings.getMember("sqlite3_error_offset") // 499
    val sqlite3_errcode = mainBindings.getMember("sqlite3_errcode") // 500
    val sqlite3_extended_errcode = mainBindings.getMember("sqlite3_extended_errcode") // 501
    val sqlite3_errstr = mainBindings.getMember("sqlite3_errstr") // 502
    val sqlite3_limit = mainBindings.getMember("sqlite3_limit") // 503
    val sqlite3_open = mainBindings.getMember("sqlite3_open") // 504
    val sqlite3_open_v2 = mainBindings.getMember("sqlite3_open_v2") // 515
    val sqlite3_create_collation = mainBindings.getMember("sqlite3_create_collation") // 516
    val sqlite3_create_collation_v2 = mainBindings.getMember("sqlite3_create_collation_v2") // 517
    val sqlite3_collation_needed = mainBindings.getMember("sqlite3_collation_needed") // 519
    val sqlite3_get_autocommit = mainBindings.getMember("sqlite3_get_autocommit") // 520
    val sqlite3_table_column_metadata = mainBindings.getMember("sqlite3_table_column_metadata") // 521
    val sqlite3_extended_result_codes = mainBindings.getMember("sqlite3_extended_result_codes") // 527
    val sqlite3_uri_key = mainBindings.getMember("sqlite3_uri_key") // 541
    val sqlite3_uri_int64 = mainBindings.getMember("sqlite3_uri_int64") // 544
    val sqlite3_db_name = mainBindings.getMember("sqlite3_db_name") // 546
    val sqlite3_db_filename = mainBindings.getMember("sqlite3_db_filename") // 547
    val sqlite3_db_readonly = mainBindings.getMember("sqlite3_db_readonly")
    val sqlite3_compileoption_used = mainBindings.getMember("sqlite3_compileoption_used") // 549
    val sqlite3_compileoption_get = mainBindings.getMember("sqlite3_compileoption_get") // 550

    val sqlite3session_diff = mainBindings.getMember("sqlite3session_diff") // 551
    val sqlite3session_attach = mainBindings.getMember("sqlite3session_attach") // 566
    val sqlite3session_create = mainBindings.getMember("sqlite3session_create") // 570
    val sqlite3session_delete = mainBindings.getMember("sqlite3session_delete") // 572
    val sqlite3session_table_filter = mainBindings.getMember("sqlite3session_table_filter") // 574
    val sqlite3session_changeset = mainBindings.getMember("sqlite3session_changeset") // 575
    val sqlite3session_changeset_strm = mainBindings.getMember("sqlite3session_changeset_strm") // 586
    val sqlite3session_patchset_strm = mainBindings.getMember("sqlite3session_patchset_strm") // 587
    val sqlite3session_patchset = mainBindings.getMember("sqlite3session_patchset") // 588
    val sqlite3session_enable = mainBindings.getMember("sqlite3session_enable") // 589
    val sqlite3session_indirect = mainBindings.getMember("sqlite3session_indirect") // 590
    val sqlite3session_isempty = mainBindings.getMember("sqlite3session_isempty") // 591
    val sqlite3session_memory_used = mainBindings.getMember("sqlite3session_memory_used") // 592
    val sqlite3session_object_config = mainBindings.getMember("sqlite3session_object_config") // 593
    val sqlite3session_changeset_size = mainBindings.getMember("sqlite3session_changeset_size") // 594

    val sqlite3changeset_start = mainBindings.getMember("sqlite3changeset_start") // 595
    val sqlite3changeset_start_v2 = mainBindings.getMember("sqlite3changeset_start_v2") // 597
    val sqlite3changeset_start_strm = mainBindings.getMember("sqlite3changeset_start_strm") // 598
    val sqlite3changeset_start_v2_strm = mainBindings.getMember("sqlite3changeset_start_v2_strm") // 599
    val sqlite3changeset_next = mainBindings.getMember("sqlite3changeset_next") // 600
    val sqlite3changeset_op = mainBindings.getMember("sqlite3changeset_op") // 608
    val sqlite3changeset_pk = mainBindings.getMember("sqlite3changeset_pk") // 609
    val sqlite3changeset_old = mainBindings.getMember("sqlite3changeset_old") // 610
    val sqlite3changeset_new = mainBindings.getMember("sqlite3changeset_new") // 611
    val sqlite3changeset_conflict = mainBindings.getMember("sqlite3changeset_conflict") // 612
    val sqlite3changeset_fk_conflicts = mainBindings.getMember("sqlite3changeset_fk_conflicts") // 613
    val sqlite3changeset_finalize = mainBindings.getMember("sqlite3changeset_finalize") // 614
    val sqlite3changeset_invert = mainBindings.getMember("sqlite3changeset_invert") // 615
    val sqlite3changeset_invert_strm = mainBindings.getMember("sqlite3changeset_invert_strm") // 618
    val sqlite3changeset_apply_v2 = mainBindings.getMember("sqlite3changeset_apply_v2") // 619
    val sqlite3changeset_apply = mainBindings.getMember("sqlite3changeset_apply") // 629
    val sqlite3changeset_apply_v2_strm = mainBindings.getMember("sqlite3changeset_apply_v2_strm") // 630
    val sqlite3changeset_apply_strm = mainBindings.getMember("sqlite3changeset_apply_strm") // 631
    val sqlite3changegroup_new = mainBindings.getMember("sqlite3changegroup_new") // 632
    val sqlite3changegroup_add = mainBindings.getMember("sqlite3changegroup_add") // 633
    val sqlite3changegroup_output = mainBindings.getMember("sqlite3changegroup_output") // 645
    val sqlite3changegroup_add_strm = mainBindings.getMember("sqlite3changegroup_add_strm") // 647
    val sqlite3changegroup_output_strm = mainBindings.getMember("sqlite3changegroup_output_strm") // 648
    val sqlite3changegroup_delete = mainBindings.getMember("sqlite3changegroup_delete") // 649
    val sqlite3changeset_concat = mainBindings.getMember("sqlite3changeset_concat") // 650
    val sqlite3changeset_concat_strm = mainBindings.getMember("sqlite3changeset_concat_strm") // 651
    val sqlite3session_config = mainBindings.getMember("sqlite3session_config") // 652
    val sqlite3_sourceid = mainBindings.getMember("sqlite3_sourceid") // 653

    val sqlite3_wasm_pstack_ptr = mainBindings.getMember("sqlite3__wasm_pstack_ptr") // 654
    val sqlite3_wasm_pstack_restore = mainBindings.getMember("sqlite3__wasm_pstack_restore") // 655
    val sqlite3_wasm_pstack_alloc = mainBindings.getMember("sqlite3_wasm_pstack_alloc") // 656
    val sqlite3_wasm_pstack_remaining = mainBindings.getMember("sqlite3_wasm_pstack_remaining") // 657
    val sqlite3_wasm_pstack_quota = mainBindings.getMember("sqlite3_wasm_pstack_quota") // 658
    val sqlite3_wasm_db_error = mainBindings.getMember("sqlite3_wasm_db_error") // 659
    val sqlite3_wasm_test_struct = mainBindings.getMember("sqlite3_wasm_test_struct") // 660
    val sqlite3_wasm_enum_json = mainBindings.getMember("sqlite3__wasm_enum_json") // 661
        ?: mainBindings.getMember("sqlite3_wasm_enum_json") // 661
    val sqlite3_wasm_vfs_unlink = mainBindings.getMember("sqlite3__wasm_vfs_unlink") // 662
    val sqlite3_wasm_db_vfs = mainBindings.getMember("sqlite3_wasm_db_vfs") // 663
    val sqlite3_wasm_db_reset = mainBindings.getMember("sqlite3_wasm_db_reset") // 664
    val sqlite3_wasm_db_export_chunked = mainBindings.getMember("sqlite3_wasm_db_export_chunked") // 665
    val sqlite3_wasm_db_serialize = mainBindings.getMember("sqlite3_wasm_db_serialize") // 666
    val sqlite3_wasm_vfs_create_file = mainBindings.getMember("sqlite3_wasm_vfs_create_file") // 667
    val sqlite3_wasm_posix_create_file = mainBindings.getMember("sqlite3_wasm_posix_create_file") // 669
    val sqlite3_wasm_kvvfsMakeKeyOnPstack = mainBindings.getMember("sqlite3_wasm_kvvfsMakeKeyOnPstack") // 670
    val sqlite3_wasm_kvvfs_methods = mainBindings.getMember("sqlite3_wasm_kvvfs_methods") // 672
    val sqlite3_wasm_vtab_config = mainBindings.getMember("sqlite3_wasm_vtab_config") // 673
    val sqlite3_wasm_db_config_ip = mainBindings.getMember("sqlite3_wasm_db_config_ip") // 674
    val sqlite3_wasm_db_config_pii = mainBindings.getMember("sqlite3_wasm_db_config_pii") // 675
    val sqlite3_wasm_db_config_s = mainBindings.getMember("sqlite3_wasm_db_config_s") // 676
    val sqlite3_wasm_config_i = mainBindings.getMember("sqlite3_wasm_config_i") // 677
    val sqlite3_wasm_config_ii = mainBindings.getMember("sqlite3_wasm_config_ii") // 678
    val sqlite3_wasm_config_j = mainBindings.getMember("sqlite3_wasm_config_j") // 679
    val sqlite3_wasm_init_wasmfs = mainBindings.getMember("sqlite3_wasm_init_wasmfs") // 680
    val sqlite3_wasm_test_intptr = mainBindings.getMember("sqlite3_wasm_test_intptr") // 681
    val sqlite3_wasm_test_voidptr = mainBindings.getMember("sqlite3_wasm_test_voidptr") // 682
    val sqlite3_wasm_test_int64_max = mainBindings.getMember("sqlite3_wasm_test_int64_max") // 683
    val sqlite3_wasm_test_int64_min = mainBindings.getMember("sqlite3_wasm_test_int64_min") // 684
    val sqlite3_wasm_test_int64_times2 = mainBindings.getMember("sqlite3_wasm_test_int64_times2") // 685
    val sqlite3_wasm_test_int64_minmax = mainBindings.getMember("sqlite3_wasm_test_int64_minmax") // 686
    val sqlite3_wasm_test_int64ptr = mainBindings.getMember("sqlite3_wasm_test_int64ptr") // 687
    val sqlite3_wasm_test_stack_overflow = mainBindings.getMember("sqlite3_wasm_test_stack_overflow") // 688
    val sqlite3_wasm_test_str_hello = mainBindings.getMember("sqlite3_wasm_test_str_hello") // 689
    val sqlite3_wasm_SQLTester_strglob = mainBindings.getMember("sqlite3_wasm_SQLTester_strglob") // 690

    private val memory = GraalHostMemoryImpl(envBindings.getMember("memory"))

    val memoryBindings = SqliteMemoryBindings(mainBindings, memory)

    init {
        initSqlite()
    }

    // globalThis.sqlite3InitModule
    private fun initSqlite() {
         requireNotNull(__wasm_call_ctors) {
             "__wasm_call_ctors not defined"
         }.execute()
        memoryBindings.init()
        postRun()
        // _initialize.execute()
    }

    private fun postRun() {
        val sqliteInitResult = sqlite3_initialize.execute().asInt()
        if (sqliteInitResult != SqliteErrno.SQLITE_OK.id) {
            throw SqliteException(sqliteInitResult, sqliteInitResult)
        }
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import kotlin.jvm.JvmInline

/**
 * SQLite C Interface: Database Connection Configuration Options
 *
 * https://www.sqlite.org/c3ref/c_dbconfig_defensive.html
 */
@JvmInline
public value class SqliteDbConfigParameter(
    public val id: Int,
) {
    public companion object {
        /**
         * This option is used to change the name of the "main" database schema. The sole argument is a pointer to a
         * constant UTF8 string which will become the new schema name in place of "main".
         * SQLite does not make a copy of the new main schema name string, so the application must ensure that the
         * argument passed into this DBCONFIG option is unchanged until after the database connection closes.
         *
         * Additional `sqlite3_db_config()` arguments: `const char*`
         */
        public val SQLITE_DBCONFIG_MAINDBNAME: SqliteDbConfigParameter = SqliteDbConfigParameter(1000)

        /**
         * This option takes three additional arguments that determine the lookaside memory allocator configuration
         * for the database connection.
         * The first argument (the third parameter to sqlite3_db_config() is a pointer to a memory buffer to use
         * for lookaside memory.
         * The first argument after the SQLITE_DBCONFIG_LOOKASIDE verb may be NULL in which case SQLite will allocate
         * the lookaside buffer itself using sqlite3_malloc().
         * The second argument is the size of each lookaside buffer slot.
         * The third argument is the number of slots.
         * The size of the buffer in the first argument must be greater than or equal to the product of the second
         * and third arguments. The buffer must be aligned to an 8-byte boundary.
         * If the second argument to SQLITE_DBCONFIG_LOOKASIDE is not a multiple of 8, it is internally rounded down
         * to the next smaller multiple of 8. The lookaside memory configuration for a database connection can only
         * be changed when that connection is not currently using lookaside memory, or in other words when the
         * "current value" returned by sqlite3_db_status(D,SQLITE_DBSTATUS_LOOKASIDE_USED,...) is zero.
         * Any attempt to change the lookaside memory configuration when lookaside memory is in use leaves the
         * configuration unchanged and returns SQLITE_BUSY.
         *
         * Additional `sqlite3_db_config()` arguments: `void* int int`
         */
        public val SQLITE_DBCONFIG_LOOKASIDE: SqliteDbConfigParameter = SqliteDbConfigParameter(1001)

        /**
         * This option is used to enable or disable the enforcement of foreign key constraints. There should be two
         * additional arguments.
         * The first argument is an integer which is 0 to disable FK enforcement, positive to enable FK enforcement
         * or negative to leave FK enforcement unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether FK
         * enforcement is off or on following this call. The second parameter may be a NULL pointer, in which case
         * the FK enforcement setting is not reported back.
         *
         * Additional `sqlite3_db_config()` arguments: `int int* `
         */
        public val SQLITE_DBCONFIG_ENABLE_FKEY: SqliteDbConfigParameter = SqliteDbConfigParameter(1002)

        /**
         * This option is used to enable or disable triggers. There should be two additional arguments.
         * The first argument is an integer which is 0 to disable triggers, positive to enable triggers or negative
         * to leave the setting unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether triggers
         * are disabled or enabled following this call. The second parameter may be a NULL pointer,
         * in which case the trigger setting is not reported back.
         * Originally this option disabled all triggers. However, since SQLite version 3.35.0, TEMP triggers are
         * still allowed even if this option is off. So, in other words, this option now only disables triggers in the
         * main database schema or in the schemas of ATTACH-ed databases.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_ENABLE_TRIGGER: SqliteDbConfigParameter = SqliteDbConfigParameter(1003)

        /**
         * This option is used to enable or disable the fts3_tokenizer() function which is part of the FTS3
         * full-text search engine extension. There should be two additional arguments.
         * The first argument is an integer which is 0 to disable fts3_tokenizer() or positive to enable
         * fts3_tokenizer() or negative to leave the setting unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether
         * fts3_tokenizer is disabled or enabled following this call. The second parameter may be a NULL pointer,
         * in which case the new setting is not reported back.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_ENABLE_FTS3_TOKENIZER: SqliteDbConfigParameter = SqliteDbConfigParameter(1004)

        /**
         * This option is used to enable or disable the sqlite3_load_extension() interface independently of the l
         * oad_extension() SQL function. The sqlite3_enable_load_extension() API enables or disables both the
         * C-API sqlite3_load_extension() and the SQL function load_extension().
         * There should be two additional arguments.
         * When the first argument to this interface is 1, then only the C-API is enabled and the SQL function
         * remains disabled. If the first argument to this interface is 0, then both the C-API and the SQL function
         * are disabled. If the first argument is -1, then no changes are made to state of either the C-API or the
         * SQL function.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether
         * sqlite3_load_extension() interface is disabled or enabled following this call. The second parameter may be a
         * NULL pointer, in which case the new setting is not reported back.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_ENABLE_LOAD_EXTENSION: SqliteDbConfigParameter = SqliteDbConfigParameter(1005)

        /**
         * Usually, when a database in wal mode is closed or detached from a database handle, SQLite checks if this
         * will mean that there are now no connections at all to the database.
         * If so, it performs a checkpoint operation before closing the connection.
         * This option may be used to override this behavior.
         * The first parameter passed to this operation is an integer - positive to disable checkpoints-on-close,
         * or zero (the default) to enable them, and negative to leave the setting unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether
         * checkpoints-on-close have been disabled - 0 if they are not disabled, 1 if they are.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_NO_CKPT_ON_CLOSE: SqliteDbConfigParameter = SqliteDbConfigParameter(1006)

        /**
         * The SQLITE_DBCONFIG_ENABLE_QPSG option activates or deactivates the query planner stability guarantee (QPSG).
         * When the QPSG is active, a single SQL query statement will always use the same algorithm regardless of
         * values of bound parameters. The QPSG disables some query optimizations that look at the values of bound
         * parameters, which can make some queries slower. But the QPSG has the advantage of more predictable behavior.
         * With the QPSG active, SQLite will always use the same query plan in the field as was used during testing i
         * the lab.
         * The first argument to this setting is an integer which is 0 to disable the QPSG, positive to enable
         * QPSG, or negative to leave the setting unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether the QPSG
         * is disabled or enabled following this call.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_ENABLE_QPSG: SqliteDbConfigParameter = SqliteDbConfigParameter(1007)

        /**
         * By default, the output of EXPLAIN QUERY PLAN commands does not include output for any operations performed
         * by trigger programs. This option is used to set or clear (the default) a flag that governs this behavior.
         * The first parameter passed to this operation is an integer - positive to enable output for trigger programs,
         * or zero to disable it, or negative to leave the setting unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether
         * output-for-triggers has been disabled - 0 if it is not disabled, 1 if it is.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_TRIGGER_EQP: SqliteDbConfigParameter = SqliteDbConfigParameter(1008)

        /**
         * Set the SQLITE_DBCONFIG_RESET_DATABASE flag and then run VACUUM in order to reset a database back to an
         * empty database with no schema and no content.
         * The following process works even for a badly corrupted database file:
         * If the database connection is newly opened, make sure it has read the database schema by preparing then
         * discarding some query against the database, or calling sqlite3_table_column_metadata(), ignoring any errors.
         * This step is only necessary if the application desires to keep the database in WAL mode after the reset if
         * it was in WAL mode before the reset.
         *  * sqlite3_db_config(db, SQLITE_DBCONFIG_RESET_DATABASE, 1, 0);
         *  * sqlite3_exec(db, "VACUUM", 0, 0, 0);
         *  * sqlite3_db_config(db, SQLITE_DBCONFIG_RESET_DATABASE, 0, 0);
         * Because resetting a database is destructive and irreversible, the process requires the use of this obscure
         * API and multiple steps to help ensure that it does not happen by accident.
         * Because this feature must be capable of resetting corrupt databases, and shutting down virtual tables may
         * require access to that corrupt storage, the library must abandon any installed virtual tables without
         * calling their xDestroy() methods.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_RESET_DATABASE: SqliteDbConfigParameter = SqliteDbConfigParameter(1009)

        /**
         * The SQLITE_DBCONFIG_DEFENSIVE option activates or deactivates the "defensive" flag for a database connection.
         * When the defensive flag is enabled, language features that allow ordinary SQL to deliberately corrupt the
         * database file are disabled. The disabled features include but are not limited to the following:
         *  * The PRAGMA writable_schema=ON statement.
         *  * The PRAGMA journal_mode=OFF statement.
         *  * The PRAGMA schema_version=N statement.
         *  * Writes to the sqlite_dbpage virtual table.
         *  * Direct writes to shadow tables.
         *
         *  Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_DEFENSIVE: SqliteDbConfigParameter = SqliteDbConfigParameter(1010)

        /**
         * The SQLITE_DBCONFIG_WRITABLE_SCHEMA option activates or deactivates the "writable_schema" flag.
         * This has the same effect and is logically equivalent to setting PRAGMA writable_schema=ON or
         * PRAGMA writable_schema=OFF.
         * The first argument to this setting is an integer which is 0 to disable the writable_schema,
         * positive to enable writable_schema, or negative to leave the setting unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether the
         * writable_schema is enabled or disabled following this call.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_WRITABLE_SCHEMA: SqliteDbConfigParameter = SqliteDbConfigParameter(1011)

        /**
         * The SQLITE_DBCONFIG_LEGACY_ALTER_TABLE option activates or deactivates the legacy behavior of the
         * ALTER TABLE RENAME command such it behaves as it did prior to version 3.24.0 (2018-06-04).
         * See the "Compatibility Notice" on the ALTER TABLE RENAME documentation for additional information.
         * This feature can also be turned on and off using the PRAGMA legacy_alter_table statement.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_LEGACY_ALTER_TABLE: SqliteDbConfigParameter = SqliteDbConfigParameter(1012)

        /**
         * The SQLITE_DBCONFIG_DQS_DML option activates or deactivates the legacy double-quoted string literal
         * misfeature for DML statements only, that is DELETE, INSERT, SELECT, and UPDATE statements.
         * The default value of this setting is determined by the -DSQLITE_DQS compile-time option.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_DQS_DML: SqliteDbConfigParameter = SqliteDbConfigParameter(1013)

        /**
         * The SQLITE_DBCONFIG_DQS option activates or deactivates the legacy double-quoted string literal misfeature
         * for DDL statements, such as CREATE TABLE and CREATE INDEX.
         * The default value of this setting is determined by the -DSQLITE_DQS compile-time option.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_DQS_DDL: SqliteDbConfigParameter = SqliteDbConfigParameter(1014)

        /**
         * This option is used to enable or disable views. There should be two additional arguments.
         * The first argument is an integer which is 0 to disable views, positive to enable views or negative to leave
         * the setting unchanged.
         * The second parameter is a pointer to an integer into which is written 0 or 1 to indicate whether views are
         * disabled or enabled following this call. The second parameter may be a NULL pointer, in which case the view
         * setting is not reported back.
         * Originally this option disabled all views. However, since SQLite version 3.35.0, TEMP views are still
         * allowed even if this option is off. So, in other words, this option now only disables views in the main
         * database schema or in the schemas of ATTACH-ed databases.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_ENABLE_VIEW: SqliteDbConfigParameter = SqliteDbConfigParameter(1015)

        /**
         * The SQLITE_DBCONFIG_LEGACY_FILE_FORMAT option activates or deactivates the legacy file format flag.
         * When activated, this flag causes all newly created database file to have a schema format version number
         * (the 4-byte integer found at offset 44 into the database header) of 1. This in turn means that the
         * resulting database file will be readable and writable by any SQLite version back to 3.0.0 (2004-06-18).
         * Without this setting, newly created databases are generally not understandable by SQLite versions prior to
         * 3.3.0 (2006-01-11). As these words are written, there is now scarcely any need to generate database files
         * that are compatible all the way back to version 3.0.0, and so this setting is of little practical use, but
         * is provided so that SQLite can continue to claim the ability to generate new database files that are
         * compatible with version 3.0.0.
         * Note that when the SQLITE_DBCONFIG_LEGACY_FILE_FORMAT setting is on, the VACUUM command will fail with an
         * obscure error when attempting to process a table with generated columns and a descending index.
         * This is not considered a bug since SQLite versions 3.3.0 and earlier do not support either generated columns
         * or descending indexes.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_LEGACY_FILE_FORMAT: SqliteDbConfigParameter = SqliteDbConfigParameter(1016)

        /**
         * The SQLITE_DBCONFIG_TRUSTED_SCHEMA option tells SQLite to assume that database schemas are untainted by
         * malicious content.
         * When the SQLITE_DBCONFIG_TRUSTED_SCHEMA option is disabled, SQLite takes additional defensive steps to
         * protect the application from harm including:
         *  * Prohibit the use of SQL functions inside triggers, views, CHECK constraints, DEFAULT clauses, expression
         *  indexes, partial indexes, or generated columns unless those functions are tagged with SQLITE_INNOCUOUS.
         *  * Prohibit the use of virtual tables inside of triggers or views unless those virtual tables are tagged
         *  with SQLITE_VTAB_INNOCUOUS.
         * This setting defaults to "on" for legacy compatibility, however all applications are advised to turn it off
         * if possible. This setting can also be controlled using the PRAGMA trusted_schema statement.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_TRUSTED_SCHEMA: SqliteDbConfigParameter = SqliteDbConfigParameter(1017)

        /**
         * The SQLITE_DBCONFIG_STMT_SCANSTATUS option is only useful in SQLITE_ENABLE_STMT_SCANSTATUS builds.
         * In this case, it sets or clears a flag that enables collection of the sqlite3_stmt_scanstatus_v2()
         * statistics.
         * For statistics to be collected, the flag must be set on the database handle both when the SQL statement is
         * prepared and when it is stepped. The flag is set (collection of statistics is enabled) by default.
         * This option takes two arguments: an integer and a pointer to an integer.
         * The first argument is 1, 0, or -1 to enable, disable, or leave unchanged the statement scanstatus option.
         * If the second argument is not NULL, then the value of the statement scanstatus setting after processing the
         * first argument is written into the integer that the second argument points to.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_STMT_SCANSTATUS: SqliteDbConfigParameter = SqliteDbConfigParameter(1018)

        /**
         * The SQLITE_DBCONFIG_REVERSE_SCANORDER option changes the default order in which tables and indexes are
         * scanned so that the scans start at the end and work toward the beginning rather than starting at the
         * beginning and working toward the end.
         * Setting SQLITE_DBCONFIG_REVERSE_SCANORDER is the same as setting PRAGMA reverse_unordered_selects.
         * This option takes two arguments which are an integer and a pointer to an integer.
         * The first argument is 1, 0, or -1 to enable, disable, or leave unchanged the reverse scan order flag,
         * respectively. If the second argument is not NULL, then 0 or 1 is written into the integer that the second
         * argument points to depending on if the reverse scan order flag is set after processing the first argument.
         *
         * Additional `sqlite3_db_config()` arguments: `int int*`
         */
        public val SQLITE_DBCONFIG_REVERSE_SCANORDER: SqliteDbConfigParameter = SqliteDbConfigParameter(1019)

        /**
         * Largest DBCONFIG
         */
        public val SQLITE_DBCONFIG_MAX: SqliteDbConfigParameter = SQLITE_DBCONFIG_REVERSE_SCANORDER
    }
}

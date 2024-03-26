/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.sqlite

import ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.SqliteConfigurationOptions.DefaultUnixVfs.UNIX_NONE

public object SqliteConfigurationOptions {
    /**
     * Build configuration from https://github.com/requery/sqlite-android.git
     *
     *  NOTE the following flags,
     *  SQLITE_TEMP_STORE=3 causes all TEMP files to go into RAM. and thats the behavior we want
     *  SQLITE_ENABLE_FTS3  enables usage of FTS3 - NOT FTS1 or 2.
     *  SQLITE_DEFAULT_AUTOVACUUM=1 causes the databases to be subject to auto-vacuum
     */
    public val requeryAndroidConfig: List<String> = listOf(
        "-DHAVE_USLEEP=1",
        "-DNDEBUG=1",
        "-DSQLITE_DEFAULT_AUTOVACUUM=1",
        "-DSQLITE_DEFAULT_FILE_FORMAT=4",
        "-DSQLITE_DEFAULT_FILE_PERMISSIONS=0600",
        "-DSQLITE_DEFAULT_JOURNAL_SIZE_LIMIT=1048576",
        "-DSQLITE_DEFAULT_MEMSTATUS=0",
        "-DSQLITE_ENABLE_BATCH_ATOMIC_WRITE",
        "-DSQLITE_ENABLE_FTS3",
        "-DSQLITE_ENABLE_FTS3_PARENTHESIS",
        "-DSQLITE_ENABLE_FTS4",
        "-DSQLITE_ENABLE_FTS4_PARENTHESIS",
        "-DSQLITE_ENABLE_FTS5",
        "-DSQLITE_ENABLE_FTS5_PARENTHESIS",
        "-DSQLITE_ENABLE_JSON1",
        "-DSQLITE_ENABLE_MEMORY_MANAGEMENT=1",
        "-DSQLITE_ENABLE_RTREE=1",
        "-DSQLITE_HAVE_ISNAN",
        "-DSQLITE_MAX_EXPR_DEPTH=0",
        "-DSQLITE_OMIT_COMPILEOPTION_DIAGS",
        "-DSQLITE_POWERSAFE_OVERWRITE=1",
        "-DSQLITE_TEMP_STORE=3",
        "-DSQLITE_THREADSAFE=2",
        "-DSQLITE_UNTESTABLE",
        "-DSQLITE_USE_ALLOCA",
        "-O3",
    )

    /**
     * Build configuration from sqlite3-wasm
     * https://sqlite.org/src/file?name=ext/wasm/GNUmakefile&ci=trunk
     */
    public fun wasmConfig(
        defaultUnixVfs: DefaultUnixVfs = UNIX_NONE,
    ): List<String> = listOf(
        defaultUnixVfs.sqliteBuildOption,
        "-DSQLITE_ENABLE_BYTECODE_VTAB",
        "-DSQLITE_ENABLE_DBPAGE_VTAB",
        "-DSQLITE_ENABLE_DBSTAT_VTAB",
        "-DSQLITE_ENABLE_EXPLAIN_COMMENTS",
        "-DSQLITE_ENABLE_FTS5",
        "-DSQLITE_ENABLE_OFFSET_SQL_FUNC",
        "-DSQLITE_ENABLE_RTREE",
        "-DSQLITE_ENABLE_STMTVTAB",
        "-DSQLITE_ENABLE_UNKNOWN_SQL_FUNCTION",
        "-DSQLITE_OMIT_DEPRECATED",
        "-DSQLITE_OMIT_LOAD_EXTENSION",
        "-DSQLITE_OMIT_SHARED_CACHE",
        "-DSQLITE_OMIT_UTF16",
        "-DSQLITE_OS_KV_OPTIONAL=1",
        "-DSQLITE_TEMP_STORE=2",
        "-DSQLITE_THREADSAFE=0",
        "-DSQLITE_USE_URI=1",
        "-DSQLITE_WASM_ENABLE_C_TESTS",
    )

    public enum class DefaultUnixVfs(public val id: String) {
        UNIX("unix"),
        UNIX_DOTFILE("unix-dotfile"),
        UNIX_EXCL("unix-excl"),
        UNIX_NONE("unix-none"),
        UNIX_NAMEDSEM("unix-namedsem"),
        ;

        public val sqliteBuildOption: String get() = """-DSQLITE_DEFAULT_UNIX_VFS="${this.id}""""
    }
}

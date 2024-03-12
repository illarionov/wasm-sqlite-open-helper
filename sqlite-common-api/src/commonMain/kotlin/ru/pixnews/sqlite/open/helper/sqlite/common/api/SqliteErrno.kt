/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.sqlite.common.api

import kotlin.jvm.JvmInline

/**
 * SQLite C Interface: Result Codes
 *
 * https://www.sqlite.org/c3ref/c_abort.html
 */
@JvmInline
@Suppress("BLANK_LINE_BETWEEN_PROPERTIES")
public value class SqliteErrno(
    public val id: Int,
) {
    public companion object {
        /**
         * No error occurred. System call completed successfully.
         */
        public val SQLITE_OK: SqliteErrno = SqliteErrno(0)

        /**
         * Generic error
         */
        public val SQLITE_ERROR: SqliteErrno = SqliteErrno(1)

        /**
         * Internal logic error in SQLite
         */
        public val SQLITE_INTERNAL: SqliteErrno = SqliteErrno(2)

        /**
         * Access permission denied
         */
        public val SQLITE_PERM: SqliteErrno = SqliteErrno(3)

        /**
         * Callback routine requested an abort
         */
        public val SQLITE_ABORT: SqliteErrno = SqliteErrno(4)

        /**
         * The database file is locked
         */
        public val SQLITE_BUSY: SqliteErrno = SqliteErrno(5)

        /**
         * A table in the database is locked
         */
        public val SQLITE_LOCKED: SqliteErrno = SqliteErrno(6)

        /**
         * A malloc() failed
         */
        public val SQLITE_NOMEM: SqliteErrno = SqliteErrno(7)

        /**
         * Attempt to write a readonly database
         */
        public val SQLITE_READONLY: SqliteErrno = SqliteErrno(8)

        /**
         * Operation terminated by sqlite3_interrupt()
         */
        public val SQLITE_INTERRUPT: SqliteErrno = SqliteErrno(9)

        /**
         * Some kind of disk I/O error occurred
         */
        public val SQLITE_IOERR: SqliteErrno = SqliteErrno(10)

        /**
         * The database disk image is malformed
         */
        public val SQLITE_CORRUPT: SqliteErrno = SqliteErrno(11)

        /**
         * Unknown opcode in sqlite3_file_control()
         */
        public val SQLITE_NOTFOUND: SqliteErrno = SqliteErrno(12)

        /**
         * Insertion failed because database is full
         */
        public val SQLITE_FULL: SqliteErrno = SqliteErrno(13)

        /**
         * Unable to open the database file
         */
        public val SQLITE_CANTOPEN: SqliteErrno = SqliteErrno(14)

        /**
         * Database lock protocol error
         */
        public val SQLITE_PROTOCOL: SqliteErrno = SqliteErrno(15)

        /**
         * Internal use only
         */
        public val SQLITE_EMPTY: SqliteErrno = SqliteErrno(16)

        /**
         * The database schema changed
         */
        public val SQLITE_SCHEMA: SqliteErrno = SqliteErrno(17)

        /**
         * String or BLOB exceeds size limit
         */
        public val SQLITE_TOOBIG: SqliteErrno = SqliteErrno(18)

        /**
         * Abort due to constraint violation
         */
        public val SQLITE_CONSTRAINT: SqliteErrno = SqliteErrno(19)

        /**
         * Data type mismatch
         */
        public val SQLITE_MISMATCH: SqliteErrno = SqliteErrno(20)

        /**
         * Library used incorrectly
         */
        public val SQLITE_MISUSE: SqliteErrno = SqliteErrno(21)

        /**
         * Uses OS features not supported on host
         */
        public val SQLITE_NOLFS: SqliteErrno = SqliteErrno(22)

        /**
         * Authorization denied
         */
        public val SQLITE_AUTH: SqliteErrno = SqliteErrno(23)

        /**
         * Not used
         */
        public val SQLITE_FORMAT: SqliteErrno = SqliteErrno(24)

        /**
         * 2nd parameter to sqlite3_bind out of range
         */
        public val SQLITE_RANGE: SqliteErrno = SqliteErrno(25)

        /**
         * File opened that is not a database file
         */
        public val SQLITE_NOTADB: SqliteErrno = SqliteErrno(26)

        /**
         * Notifications from sqlite3_log()
         */
        public val SQLITE_NOTICE: SqliteErrno = SqliteErrno(27)

        /**
         * Warnings from sqlite3_log()
         */
        public val SQLITE_WARNING: SqliteErrno = SqliteErrno(28)

        /**
         * sqlite3_step() has another row ready
         */
        public val SQLITE_ROW: SqliteErrno = SqliteErrno(100)

        /**
         * sqlite3_step() has finished executing
         */
        public val SQLITE_DONE: SqliteErrno = SqliteErrno(101)

        public val SQLITE_ERROR_MISSING_COLLSEQ: SqliteErrno = SqliteErrno((SQLITE_ERROR.id.or(1.shl(8))))
        public val SQLITE_ERROR_RETRY: SqliteErrno = SqliteErrno((SQLITE_ERROR.id.or(2.shl(8))))
        public val SQLITE_ERROR_SNAPSHOT: SqliteErrno = SqliteErrno((SQLITE_ERROR.id.or(3.shl(8))))

        public val SQLITE_IOERR_READ: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(1.shl(8))))
        public val SQLITE_IOERR_SHORT_READ: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(2.shl(8))))
        public val SQLITE_IOERR_WRITE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(3.shl(8))))
        public val SQLITE_IOERR_FSYNC: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(4.shl(8))))
        public val SQLITE_IOERR_DIR_FSYNC: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(5.shl(8))))
        public val SQLITE_IOERR_TRUNCATE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(6.shl(8))))
        public val SQLITE_IOERR_FSTAT: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(7.shl(8))))
        public val SQLITE_IOERR_UNLOCK: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(8.shl(8))))
        public val SQLITE_IOERR_RDLOCK: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(9.shl(8))))
        public val SQLITE_IOERR_DELETE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(10.shl(8))))
        public val SQLITE_IOERR_BLOCKED: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(11.shl(8))))
        public val SQLITE_IOERR_NOMEM: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(12.shl(8))))
        public val SQLITE_IOERR_ACCESS: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(13.shl(8))))
        public val SQLITE_IOERR_CHECKRESERVEDLOCK: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(14.shl(8))))
        public val SQLITE_IOERR_LOCK: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(15.shl(8))))
        public val SQLITE_IOERR_CLOSE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(16.shl(8))))
        public val SQLITE_IOERR_DIR_CLOSE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(17.shl(8))))
        public val SQLITE_IOERR_SHMOPEN: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(18.shl(8))))
        public val SQLITE_IOERR_SHMSIZE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(19.shl(8))))
        public val SQLITE_IOERR_SHMLOCK: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(20.shl(8))))
        public val SQLITE_IOERR_SHMMAP: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(21.shl(8))))
        public val SQLITE_IOERR_SEEK: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(22.shl(8))))
        public val SQLITE_IOERR_DELETE_NOENT: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(23.shl(8))))
        public val SQLITE_IOERR_MMAP: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(24.shl(8))))
        public val SQLITE_IOERR_GETTEMPPATH: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(25.shl(8))))
        public val SQLITE_IOERR_CONVPATH: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(26.shl(8))))
        public val SQLITE_IOERR_VNODE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(27.shl(8))))
        public val SQLITE_IOERR_AUTH: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(28.shl(8))))
        public val SQLITE_IOERR_BEGIN_ATOMIC: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(29.shl(8))))
        public val SQLITE_IOERR_COMMIT_ATOMIC: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(30.shl(8))))
        public val SQLITE_IOERR_ROLLBACK_ATOMIC: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(31.shl(8))))
        public val SQLITE_IOERR_DATA: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(32.shl(8))))
        public val SQLITE_IOERR_CORRUPTFS: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(33.shl(8))))
        public val SQLITE_IOERR_IN_PAGE: SqliteErrno = SqliteErrno((SQLITE_IOERR.id.or(34.shl(8))))

        public val SQLITE_LOCKED_SHAREDCACHE: SqliteErrno = SqliteErrno((SQLITE_LOCKED.id.or(1.shl(8))))
        public val SQLITE_LOCKED_VTAB: SqliteErrno = SqliteErrno((SQLITE_LOCKED.id.or(2.shl(8))))

        public val SQLITE_BUSY_RECOVERY: SqliteErrno = SqliteErrno((SQLITE_BUSY.id.or(1.shl(8))))
        public val SQLITE_BUSY_SNAPSHOT: SqliteErrno = SqliteErrno((SQLITE_BUSY.id.or(2.shl(8))))
        public val SQLITE_BUSY_TIMEOUT: SqliteErrno = SqliteErrno((SQLITE_BUSY.id.or(3.shl(8))))

        public val SQLITE_CANTOPEN_NOTEMPDIR: SqliteErrno = SqliteErrno((SQLITE_CANTOPEN.id.or(1.shl(8))))
        public val SQLITE_CANTOPEN_ISDIR: SqliteErrno = SqliteErrno((SQLITE_CANTOPEN.id.or(2.shl(8))))
        public val SQLITE_CANTOPEN_FULLPATH: SqliteErrno = SqliteErrno((SQLITE_CANTOPEN.id.or(3.shl(8))))
        public val SQLITE_CANTOPEN_CONVPATH: SqliteErrno = SqliteErrno((SQLITE_CANTOPEN.id.or(4.shl(8))))

        /* Not Used */
        public val SQLITE_CANTOPEN_DIRTYWAL: SqliteErrno = SqliteErrno((SQLITE_CANTOPEN.id.or(5.shl(8))))

        public val SQLITE_CANTOPEN_SYMLINK: SqliteErrno = SqliteErrno((SQLITE_CANTOPEN.id.or(6.shl(8))))

        public val SQLITE_CORRUPT_VTAB: SqliteErrno = SqliteErrno((SQLITE_CORRUPT.id.or(1.shl(8))))
        public val SQLITE_CORRUPT_SEQUENCE: SqliteErrno = SqliteErrno((SQLITE_CORRUPT.id.or(2.shl(8))))
        public val SQLITE_CORRUPT_INDEX: SqliteErrno = SqliteErrno((SQLITE_CORRUPT.id.or(3.shl(8))))

        public val SQLITE_READONLY_RECOVERY: SqliteErrno = SqliteErrno((SQLITE_READONLY.id.or(1.shl(8))))
        public val SQLITE_READONLY_CANTLOCK: SqliteErrno = SqliteErrno((SQLITE_READONLY.id.or(2.shl(8))))
        public val SQLITE_READONLY_ROLLBACK: SqliteErrno = SqliteErrno((SQLITE_READONLY.id.or(3.shl(8))))
        public val SQLITE_READONLY_DBMOVED: SqliteErrno = SqliteErrno((SQLITE_READONLY.id.or(4.shl(8))))
        public val SQLITE_READONLY_CANTINIT: SqliteErrno = SqliteErrno((SQLITE_READONLY.id.or(5.shl(8))))
        public val SQLITE_READONLY_DIRECTORY: SqliteErrno = SqliteErrno((SQLITE_READONLY.id.or(6.shl(8))))

        public val SQLITE_ABORT_ROLLBACK: SqliteErrno = SqliteErrno((SQLITE_ABORT.id.or(2.shl(8))))

        public val SQLITE_CONSTRAINT_CHECK: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(1.shl(8))))
        public val SQLITE_CONSTRAINT_COMMITHOOK: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(2.shl(8))))
        public val SQLITE_CONSTRAINT_FOREIGNKEY: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(3.shl(8))))
        public val SQLITE_CONSTRAINT_FUNCTION: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(4.shl(8))))
        public val SQLITE_CONSTRAINT_NOTNULL: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(5.shl(8))))
        public val SQLITE_CONSTRAINT_PRIMARYKEY: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(6.shl(8))))
        public val SQLITE_CONSTRAINT_TRIGGER: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(7.shl(8))))
        public val SQLITE_CONSTRAINT_UNIQUE: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(8.shl(8))))
        public val SQLITE_CONSTRAINT_VTAB: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(9.shl(8))))
        public val SQLITE_CONSTRAINT_ROWID: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(10.shl(8))))
        public val SQLITE_CONSTRAINT_PINNED: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(11.shl(8))))
        public val SQLITE_CONSTRAINT_DATATYPE: SqliteErrno = SqliteErrno((SQLITE_CONSTRAINT.id.or(12.shl(8))))

        public val SQLITE_NOTICE_RECOVER_WAL: SqliteErrno = SqliteErrno((SQLITE_NOTICE.id.or(1.shl(8))))
        public val SQLITE_NOTICE_RECOVER_ROLLBACK: SqliteErrno = SqliteErrno((SQLITE_NOTICE.id.or(2.shl(8))))
        public val SQLITE_NOTICE_RBU: SqliteErrno = SqliteErrno((SQLITE_NOTICE.id.or(3.shl(8))))

        public val SQLITE_WARNING_AUTOINDEX: SqliteErrno = SqliteErrno((SQLITE_WARNING.id.or(1.shl(8))))

        public val SQLITE_AUTH_USER: SqliteErrno = SqliteErrno((SQLITE_AUTH.id.or(1.shl(8))))

        public val SQLITE_OK_LOAD_PERMANENTLY: SqliteErrno = SqliteErrno((SQLITE_OK.id.or(1.shl(8))))

        /* internal use only */
        public val SQLITE_OK_SYMLINK: SqliteErrno = SqliteErrno((SQLITE_OK.id.or(2.shl(8))))

        public val entriesMap: Map<Int, SqliteErrno> = listOf(
            SQLITE_OK,
            SQLITE_ERROR,
            SQLITE_INTERNAL,
            SQLITE_PERM,
            SQLITE_ABORT,
            SQLITE_BUSY,
            SQLITE_LOCKED,
            SQLITE_NOMEM,
            SQLITE_READONLY,
            SQLITE_INTERRUPT,
            SQLITE_IOERR,
            SQLITE_CORRUPT,
            SQLITE_NOTFOUND,
            SQLITE_FULL,
            SQLITE_CANTOPEN,
            SQLITE_PROTOCOL,
            SQLITE_EMPTY,
            SQLITE_SCHEMA,
            SQLITE_TOOBIG,
            SQLITE_CONSTRAINT,
            SQLITE_MISMATCH,
            SQLITE_MISUSE,
            SQLITE_NOLFS,
            SQLITE_AUTH,
            SQLITE_FORMAT,
            SQLITE_RANGE,
            SQLITE_NOTADB,
            SQLITE_NOTICE,
            SQLITE_WARNING,
            SQLITE_ROW,
            SQLITE_DONE,
            SQLITE_ERROR_MISSING_COLLSEQ,
            SQLITE_ERROR_RETRY,
            SQLITE_ERROR_SNAPSHOT,
            SQLITE_IOERR_READ,
            SQLITE_IOERR_SHORT_READ,
            SQLITE_IOERR_WRITE,
            SQLITE_IOERR_FSYNC,
            SQLITE_IOERR_DIR_FSYNC,
            SQLITE_IOERR_TRUNCATE,
            SQLITE_IOERR_FSTAT,
            SQLITE_IOERR_UNLOCK,
            SQLITE_IOERR_RDLOCK,
            SQLITE_IOERR_DELETE,
            SQLITE_IOERR_BLOCKED,
            SQLITE_IOERR_NOMEM,
            SQLITE_IOERR_ACCESS,
            SQLITE_IOERR_CHECKRESERVEDLOCK,
            SQLITE_IOERR_LOCK,
            SQLITE_IOERR_CLOSE,
            SQLITE_IOERR_DIR_CLOSE,
            SQLITE_IOERR_SHMOPEN,
            SQLITE_IOERR_SHMSIZE,
            SQLITE_IOERR_SHMLOCK,
            SQLITE_IOERR_SHMMAP,
            SQLITE_IOERR_SEEK,
            SQLITE_IOERR_DELETE_NOENT,
            SQLITE_IOERR_MMAP,
            SQLITE_IOERR_GETTEMPPATH,
            SQLITE_IOERR_CONVPATH,
            SQLITE_IOERR_VNODE,
            SQLITE_IOERR_AUTH,
            SQLITE_IOERR_BEGIN_ATOMIC,
            SQLITE_IOERR_COMMIT_ATOMIC,
            SQLITE_IOERR_ROLLBACK_ATOMIC,
            SQLITE_IOERR_DATA,
            SQLITE_IOERR_CORRUPTFS,
            SQLITE_IOERR_IN_PAGE,
            SQLITE_LOCKED_SHAREDCACHE,
            SQLITE_LOCKED_VTAB,
            SQLITE_BUSY_RECOVERY,
            SQLITE_BUSY_SNAPSHOT,
            SQLITE_BUSY_TIMEOUT,
            SQLITE_CANTOPEN_NOTEMPDIR,
            SQLITE_CANTOPEN_ISDIR,
            SQLITE_CANTOPEN_FULLPATH,
            SQLITE_CANTOPEN_CONVPATH,
            SQLITE_CANTOPEN_DIRTYWAL,
            SQLITE_CANTOPEN_SYMLINK,
            SQLITE_CORRUPT_VTAB,
            SQLITE_CORRUPT_SEQUENCE,
            SQLITE_CORRUPT_INDEX,
            SQLITE_READONLY_RECOVERY,
            SQLITE_READONLY_CANTLOCK,
            SQLITE_READONLY_ROLLBACK,
            SQLITE_READONLY_DBMOVED,
            SQLITE_READONLY_CANTINIT,
            SQLITE_READONLY_DIRECTORY,
            SQLITE_ABORT_ROLLBACK,
            SQLITE_CONSTRAINT_CHECK,
            SQLITE_CONSTRAINT_COMMITHOOK,
            SQLITE_CONSTRAINT_FOREIGNKEY,
            SQLITE_CONSTRAINT_FUNCTION,
            SQLITE_CONSTRAINT_NOTNULL,
            SQLITE_CONSTRAINT_PRIMARYKEY,
            SQLITE_CONSTRAINT_TRIGGER,
            SQLITE_CONSTRAINT_UNIQUE,
            SQLITE_CONSTRAINT_VTAB,
            SQLITE_CONSTRAINT_ROWID,
            SQLITE_CONSTRAINT_PINNED,
            SQLITE_CONSTRAINT_DATATYPE,
            SQLITE_NOTICE_RECOVER_WAL,
            SQLITE_NOTICE_RECOVER_ROLLBACK,
            SQLITE_NOTICE_RBU,
            SQLITE_WARNING_AUTOINDEX,
            SQLITE_AUTH_USER,
            SQLITE_OK_LOAD_PERMANENTLY,
            SQLITE_OK_SYMLINK,
        ).associateBy(SqliteErrno::id)

        public fun fromErrNoCode(code: Int): SqliteErrno? = entriesMap[code]
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("BLANK_LINE_BETWEEN_PROPERTIES", "PropertyWrapping", "ArgumentListWrapping", "MaxLineLength")

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

/**
 * SQLite C Interface: Result Codes
 *
 * https://www.sqlite.org/c3ref/c_abort.html
 */
@JvmInline
public value class SqliteResultCode(
    public val id: Int,
) {
    public companion object {
        /**
         * No error occurred. System call completed successfully.
         */
        public val SQLITE_OK: SqliteResultCode = SqliteResultCode(0)

        /**
         * Generic error
         */
        public val SQLITE_ERROR: SqliteResultCode = SqliteResultCode(1)

        /**
         * Internal logic error in SQLite
         */
        public val SQLITE_INTERNAL: SqliteResultCode = SqliteResultCode(2)

        /**
         * Access permission denied
         */
        public val SQLITE_PERM: SqliteResultCode = SqliteResultCode(3)

        /**
         * Callback routine requested an abort
         */
        public val SQLITE_ABORT: SqliteResultCode = SqliteResultCode(4)

        /**
         * The database file is locked
         */
        public val SQLITE_BUSY: SqliteResultCode = SqliteResultCode(5)

        /**
         * A table in the database is locked
         */
        public val SQLITE_LOCKED: SqliteResultCode = SqliteResultCode(6)

        /**
         * A malloc() failed
         */
        public val SQLITE_NOMEM: SqliteResultCode = SqliteResultCode(7)

        /**
         * Attempt to write a readonly database
         */
        public val SQLITE_READONLY: SqliteResultCode = SqliteResultCode(8)

        /**
         * Operation terminated by sqlite3_interrupt()
         */
        public val SQLITE_INTERRUPT: SqliteResultCode = SqliteResultCode(9)

        /**
         * Some kind of disk I/O error occurred
         */
        public val SQLITE_IOERR: SqliteResultCode = SqliteResultCode(10)

        /**
         * The database disk image is malformed
         */
        public val SQLITE_CORRUPT: SqliteResultCode = SqliteResultCode(11)

        /**
         * Unknown opcode in sqlite3_file_control()
         */
        public val SQLITE_NOTFOUND: SqliteResultCode = SqliteResultCode(12)

        /**
         * Insertion failed because database is full
         */
        public val SQLITE_FULL: SqliteResultCode = SqliteResultCode(13)

        /**
         * Unable to open the database file
         */
        public val SQLITE_CANTOPEN: SqliteResultCode = SqliteResultCode(14)

        /**
         * Database lock protocol error
         */
        public val SQLITE_PROTOCOL: SqliteResultCode = SqliteResultCode(15)

        /**
         * Internal use only
         */
        public val SQLITE_EMPTY: SqliteResultCode = SqliteResultCode(16)

        /**
         * The database schema changed
         */
        public val SQLITE_SCHEMA: SqliteResultCode = SqliteResultCode(17)

        /**
         * String or BLOB exceeds size limit
         */
        public val SQLITE_TOOBIG: SqliteResultCode = SqliteResultCode(18)

        /**
         * Abort due to constraint violation
         */
        public val SQLITE_CONSTRAINT: SqliteResultCode = SqliteResultCode(19)

        /**
         * Data type mismatch
         */
        public val SQLITE_MISMATCH: SqliteResultCode = SqliteResultCode(20)

        /**
         * Library used incorrectly
         */
        public val SQLITE_MISUSE: SqliteResultCode = SqliteResultCode(21)

        /**
         * Uses OS features not supported on host
         */
        public val SQLITE_NOLFS: SqliteResultCode = SqliteResultCode(22)

        /**
         * Authorization denied
         */
        public val SQLITE_AUTH: SqliteResultCode = SqliteResultCode(23)

        /**
         * Not used
         */
        public val SQLITE_FORMAT: SqliteResultCode = SqliteResultCode(24)

        /**
         * 2nd parameter to sqlite3_bind out of range
         */
        public val SQLITE_RANGE: SqliteResultCode = SqliteResultCode(25)

        /**
         * File opened that is not a database file
         */
        public val SQLITE_NOTADB: SqliteResultCode = SqliteResultCode(26)

        /**
         * Notifications from sqlite3_log()
         */
        public val SQLITE_NOTICE: SqliteResultCode = SqliteResultCode(27)

        /**
         * Warnings from sqlite3_log()
         */
        public val SQLITE_WARNING: SqliteResultCode = SqliteResultCode(28)

        /**
         * sqlite3_step() has another row ready
         */
        public val SQLITE_ROW: SqliteResultCode = SqliteResultCode(100)

        /**
         * sqlite3_step() has finished executing
         */
        public val SQLITE_DONE: SqliteResultCode = SqliteResultCode(101)

        public val SQLITE_ERROR_MISSING_COLLSEQ: SqliteResultCode = SqliteResultCode((SQLITE_ERROR.id.or(1.shl(8))))
        public val SQLITE_ERROR_RETRY: SqliteResultCode = SqliteResultCode((SQLITE_ERROR.id.or(2.shl(8))))
        public val SQLITE_ERROR_SNAPSHOT: SqliteResultCode = SqliteResultCode((SQLITE_ERROR.id.or(3.shl(8))))

        public val SQLITE_IOERR_READ: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(1.shl(8))))
        public val SQLITE_IOERR_SHORT_READ: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(2.shl(8))))
        public val SQLITE_IOERR_WRITE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(3.shl(8))))
        public val SQLITE_IOERR_FSYNC: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(4.shl(8))))
        public val SQLITE_IOERR_DIR_FSYNC: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(5.shl(8))))
        public val SQLITE_IOERR_TRUNCATE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(6.shl(8))))
        public val SQLITE_IOERR_FSTAT: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(7.shl(8))))
        public val SQLITE_IOERR_UNLOCK: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(8.shl(8))))
        public val SQLITE_IOERR_RDLOCK: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(9.shl(8))))
        public val SQLITE_IOERR_DELETE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(10.shl(8))))
        public val SQLITE_IOERR_BLOCKED: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(11.shl(8))))
        public val SQLITE_IOERR_NOMEM: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(12.shl(8))))
        public val SQLITE_IOERR_ACCESS: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(13.shl(8))))
        public val SQLITE_IOERR_CHECKRESERVEDLOCK: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(14.shl(8))))
        public val SQLITE_IOERR_LOCK: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(15.shl(8))))
        public val SQLITE_IOERR_CLOSE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(16.shl(8))))
        public val SQLITE_IOERR_DIR_CLOSE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(17.shl(8))))
        public val SQLITE_IOERR_SHMOPEN: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(18.shl(8))))
        public val SQLITE_IOERR_SHMSIZE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(19.shl(8))))
        public val SQLITE_IOERR_SHMLOCK: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(20.shl(8))))
        public val SQLITE_IOERR_SHMMAP: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(21.shl(8))))
        public val SQLITE_IOERR_SEEK: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(22.shl(8))))
        public val SQLITE_IOERR_DELETE_NOENT: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(23.shl(8))))
        public val SQLITE_IOERR_MMAP: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(24.shl(8))))
        public val SQLITE_IOERR_GETTEMPPATH: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(25.shl(8))))
        public val SQLITE_IOERR_CONVPATH: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(26.shl(8))))
        public val SQLITE_IOERR_VNODE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(27.shl(8))))
        public val SQLITE_IOERR_AUTH: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(28.shl(8))))
        public val SQLITE_IOERR_BEGIN_ATOMIC: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(29.shl(8))))
        public val SQLITE_IOERR_COMMIT_ATOMIC: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(30.shl(8))))
        public val SQLITE_IOERR_ROLLBACK_ATOMIC: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(31.shl(8))))
        public val SQLITE_IOERR_DATA: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(32.shl(8))))
        public val SQLITE_IOERR_CORRUPTFS: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(33.shl(8))))
        public val SQLITE_IOERR_IN_PAGE: SqliteResultCode = SqliteResultCode((SQLITE_IOERR.id.or(34.shl(8))))

        public val SQLITE_LOCKED_SHAREDCACHE: SqliteResultCode = SqliteResultCode((SQLITE_LOCKED.id.or(1.shl(8))))
        public val SQLITE_LOCKED_VTAB: SqliteResultCode = SqliteResultCode((SQLITE_LOCKED.id.or(2.shl(8))))

        public val SQLITE_BUSY_RECOVERY: SqliteResultCode = SqliteResultCode((SQLITE_BUSY.id.or(1.shl(8))))
        public val SQLITE_BUSY_SNAPSHOT: SqliteResultCode = SqliteResultCode((SQLITE_BUSY.id.or(2.shl(8))))
        public val SQLITE_BUSY_TIMEOUT: SqliteResultCode = SqliteResultCode((SQLITE_BUSY.id.or(3.shl(8))))

        public val SQLITE_CANTOPEN_NOTEMPDIR: SqliteResultCode = SqliteResultCode((SQLITE_CANTOPEN.id.or(1.shl(8))))
        public val SQLITE_CANTOPEN_ISDIR: SqliteResultCode = SqliteResultCode((SQLITE_CANTOPEN.id.or(2.shl(8))))
        public val SQLITE_CANTOPEN_FULLPATH: SqliteResultCode = SqliteResultCode((SQLITE_CANTOPEN.id.or(3.shl(8))))
        public val SQLITE_CANTOPEN_CONVPATH: SqliteResultCode = SqliteResultCode((SQLITE_CANTOPEN.id.or(4.shl(8))))

        /* Not Used */
        public val SQLITE_CANTOPEN_DIRTYWAL: SqliteResultCode = SqliteResultCode((SQLITE_CANTOPEN.id.or(5.shl(8))))

        public val SQLITE_CANTOPEN_SYMLINK: SqliteResultCode = SqliteResultCode((SQLITE_CANTOPEN.id.or(6.shl(8))))

        public val SQLITE_CORRUPT_VTAB: SqliteResultCode = SqliteResultCode((SQLITE_CORRUPT.id.or(1.shl(8))))
        public val SQLITE_CORRUPT_SEQUENCE: SqliteResultCode = SqliteResultCode((SQLITE_CORRUPT.id.or(2.shl(8))))
        public val SQLITE_CORRUPT_INDEX: SqliteResultCode = SqliteResultCode((SQLITE_CORRUPT.id.or(3.shl(8))))

        public val SQLITE_READONLY_RECOVERY: SqliteResultCode = SqliteResultCode((SQLITE_READONLY.id.or(1.shl(8))))
        public val SQLITE_READONLY_CANTLOCK: SqliteResultCode = SqliteResultCode((SQLITE_READONLY.id.or(2.shl(8))))
        public val SQLITE_READONLY_ROLLBACK: SqliteResultCode = SqliteResultCode((SQLITE_READONLY.id.or(3.shl(8))))
        public val SQLITE_READONLY_DBMOVED: SqliteResultCode = SqliteResultCode((SQLITE_READONLY.id.or(4.shl(8))))
        public val SQLITE_READONLY_CANTINIT: SqliteResultCode = SqliteResultCode((SQLITE_READONLY.id.or(5.shl(8))))
        public val SQLITE_READONLY_DIRECTORY: SqliteResultCode = SqliteResultCode((SQLITE_READONLY.id.or(6.shl(8))))

        public val SQLITE_ABORT_ROLLBACK: SqliteResultCode = SqliteResultCode((SQLITE_ABORT.id.or(2.shl(8))))

        public val SQLITE_CONSTRAINT_CHECK: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(1.shl(8))))
        public val SQLITE_CONSTRAINT_COMMITHOOK: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(2.shl(8))))
        public val SQLITE_CONSTRAINT_FOREIGNKEY: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(3.shl(8))))
        public val SQLITE_CONSTRAINT_FUNCTION: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(4.shl(8))))
        public val SQLITE_CONSTRAINT_NOTNULL: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(5.shl(8))))
        public val SQLITE_CONSTRAINT_PRIMARYKEY: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(6.shl(8))))
        public val SQLITE_CONSTRAINT_TRIGGER: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(7.shl(8))))
        public val SQLITE_CONSTRAINT_UNIQUE: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(8.shl(8))))
        public val SQLITE_CONSTRAINT_VTAB: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(9.shl(8))))
        public val SQLITE_CONSTRAINT_ROWID: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(10.shl(8))))
        public val SQLITE_CONSTRAINT_PINNED: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(11.shl(8))))
        public val SQLITE_CONSTRAINT_DATATYPE: SqliteResultCode = SqliteResultCode((SQLITE_CONSTRAINT.id.or(12.shl(8))))

        public val SQLITE_NOTICE_RECOVER_WAL: SqliteResultCode = SqliteResultCode((SQLITE_NOTICE.id.or(1.shl(8))))
        public val SQLITE_NOTICE_RECOVER_ROLLBACK: SqliteResultCode = SqliteResultCode((SQLITE_NOTICE.id.or(2.shl(8))))
        public val SQLITE_NOTICE_RBU: SqliteResultCode = SqliteResultCode((SQLITE_NOTICE.id.or(3.shl(8))))

        public val SQLITE_WARNING_AUTOINDEX: SqliteResultCode = SqliteResultCode((SQLITE_WARNING.id.or(1.shl(8))))

        public val SQLITE_AUTH_USER: SqliteResultCode = SqliteResultCode((SQLITE_AUTH.id.or(1.shl(8))))

        public val SQLITE_OK_LOAD_PERMANENTLY: SqliteResultCode = SqliteResultCode((SQLITE_OK.id.or(1.shl(8))))

        /* internal use only */
        public val SQLITE_OK_SYMLINK: SqliteResultCode = SqliteResultCode((SQLITE_OK.id.or(2.shl(8))))

        public val SqliteResultCode.name: String
            get() = when (this) {
                SQLITE_OK -> "SQLITE_OK"
                SQLITE_ERROR -> "SQLITE_ERROR"
                SQLITE_INTERNAL -> "SQLITE_INTERNAL"
                SQLITE_PERM -> "SQLITE_PERM"
                SQLITE_ABORT -> "SQLITE_ABORT"
                SQLITE_BUSY -> "SQLITE_BUSY"
                SQLITE_LOCKED -> "SQLITE_LOCKED"
                SQLITE_NOMEM -> "SQLITE_NOMEM"
                SQLITE_READONLY -> "SQLITE_READONLY"
                SQLITE_INTERRUPT -> "SQLITE_INTERRUPT"
                SQLITE_IOERR -> "SQLITE_IOERR"
                SQLITE_CORRUPT -> "SQLITE_CORRUPT"
                SQLITE_NOTFOUND -> "SQLITE_NOTFOUND"
                SQLITE_FULL -> "SQLITE_FULL"
                SQLITE_CANTOPEN -> "SQLITE_CANTOPEN"
                SQLITE_PROTOCOL -> "SQLITE_PROTOCOL"
                SQLITE_EMPTY -> "SQLITE_EMPTY"
                SQLITE_SCHEMA -> "SQLITE_SCHEMA"
                SQLITE_TOOBIG -> "SQLITE_TOOBIG"
                SQLITE_CONSTRAINT -> "SQLITE_CONSTRAINT"
                SQLITE_MISMATCH -> "SQLITE_MISMATCH"
                SQLITE_MISUSE -> "SQLITE_MISUSE"
                SQLITE_NOLFS -> "SQLITE_NOLFS"
                SQLITE_AUTH -> "SQLITE_AUTH"
                SQLITE_FORMAT -> "SQLITE_FORMAT"
                SQLITE_RANGE -> "SQLITE_RANGE"
                SQLITE_NOTADB -> "SQLITE_NOTADB"
                SQLITE_NOTICE -> "SQLITE_NOTICE"
                SQLITE_WARNING -> "SQLITE_WARNING"
                SQLITE_ROW -> "SQLITE_ROW"
                SQLITE_DONE -> "SQLITE_DONE"
                SQLITE_ERROR_MISSING_COLLSEQ -> "SQLITE_ERROR_MISSING_COLLSEQ"
                SQLITE_ERROR_RETRY -> "SQLITE_ERROR_RETRY"
                SQLITE_ERROR_SNAPSHOT -> "SQLITE_ERROR_SNAPSHOT"
                SQLITE_IOERR_READ -> "SQLITE_IOERR_READ"
                SQLITE_IOERR_SHORT_READ -> "SQLITE_IOERR_SHORT_READ"
                SQLITE_IOERR_WRITE -> "SQLITE_IOERR_WRITE"
                SQLITE_IOERR_FSYNC -> "SQLITE_IOERR_FSYNC"
                SQLITE_IOERR_DIR_FSYNC -> "SQLITE_IOERR_DIR_FSYNC"
                SQLITE_IOERR_TRUNCATE -> "SQLITE_IOERR_TRUNCATE"
                SQLITE_IOERR_FSTAT -> "SQLITE_IOERR_FSTAT"
                SQLITE_IOERR_UNLOCK -> "SQLITE_IOERR_UNLOCK"
                SQLITE_IOERR_RDLOCK -> "SQLITE_IOERR_RDLOCK"
                SQLITE_IOERR_DELETE -> "SQLITE_IOERR_DELETE"
                SQLITE_IOERR_BLOCKED -> "SQLITE_IOERR_BLOCKED"
                SQLITE_IOERR_NOMEM -> "SQLITE_IOERR_NOMEM"
                SQLITE_IOERR_ACCESS -> "SQLITE_IOERR_ACCESS"
                SQLITE_IOERR_CHECKRESERVEDLOCK -> "SQLITE_IOERR_CHECKRESERVEDLOCK"
                SQLITE_IOERR_LOCK -> "SQLITE_IOERR_LOCK"
                SQLITE_IOERR_CLOSE -> "SQLITE_IOERR_CLOSE"
                SQLITE_IOERR_DIR_CLOSE -> "SQLITE_IOERR_DIR_CLOSE"
                SQLITE_IOERR_SHMOPEN -> "SQLITE_IOERR_SHMOPEN"
                SQLITE_IOERR_SHMSIZE -> "SQLITE_IOERR_SHMSIZE"
                SQLITE_IOERR_SHMLOCK -> "SQLITE_IOERR_SHMLOCK"
                SQLITE_IOERR_SHMMAP -> "SQLITE_IOERR_SHMMAP"
                SQLITE_IOERR_SEEK -> "SQLITE_IOERR_SEEK"
                SQLITE_IOERR_DELETE_NOENT -> "SQLITE_IOERR_DELETE_NOENT"
                SQLITE_IOERR_MMAP -> "SQLITE_IOERR_MMAP"
                SQLITE_IOERR_GETTEMPPATH -> "SQLITE_IOERR_GETTEMPPATH"
                SQLITE_IOERR_CONVPATH -> "SQLITE_IOERR_CONVPATH"
                SQLITE_IOERR_VNODE -> "SQLITE_IOERR_VNODE"
                SQLITE_IOERR_AUTH -> "SQLITE_IOERR_AUTH"
                SQLITE_IOERR_BEGIN_ATOMIC -> "SQLITE_IOERR_BEGIN_ATOMIC"
                SQLITE_IOERR_COMMIT_ATOMIC -> "SQLITE_IOERR_COMMIT_ATOMIC"
                SQLITE_IOERR_ROLLBACK_ATOMIC -> "SQLITE_IOERR_ROLLBACK_ATOMIC"
                SQLITE_IOERR_DATA -> "SQLITE_IOERR_DATA"
                SQLITE_IOERR_CORRUPTFS -> "SQLITE_IOERR_CORRUPTFS"
                SQLITE_IOERR_IN_PAGE -> "SQLITE_IOERR_IN_PAGE"
                SQLITE_LOCKED_SHAREDCACHE -> "SQLITE_LOCKED_SHAREDCACHE"
                SQLITE_LOCKED_VTAB -> "SQLITE_LOCKED_VTAB"
                SQLITE_BUSY_RECOVERY -> "SQLITE_BUSY_RECOVERY"
                SQLITE_BUSY_SNAPSHOT -> "SQLITE_BUSY_SNAPSHOT"
                SQLITE_BUSY_TIMEOUT -> "SQLITE_BUSY_TIMEOUT"
                SQLITE_CANTOPEN_NOTEMPDIR -> "SQLITE_CANTOPEN_NOTEMPDIR"
                SQLITE_CANTOPEN_ISDIR -> "SQLITE_CANTOPEN_ISDIR"
                SQLITE_CANTOPEN_FULLPATH -> "SQLITE_CANTOPEN_FULLPATH"
                SQLITE_CANTOPEN_CONVPATH -> "SQLITE_CANTOPEN_CONVPATH"
                SQLITE_CANTOPEN_DIRTYWAL -> "SQLITE_CANTOPEN_DIRTYWAL"
                SQLITE_CANTOPEN_SYMLINK -> "SQLITE_CANTOPEN_SYMLINK"
                SQLITE_CORRUPT_VTAB -> "SQLITE_CORRUPT_VTAB"
                SQLITE_CORRUPT_SEQUENCE -> "SQLITE_CORRUPT_SEQUENCE"
                SQLITE_CORRUPT_INDEX -> "SQLITE_CORRUPT_INDEX"
                SQLITE_READONLY_RECOVERY -> "SQLITE_READONLY_RECOVERY"
                SQLITE_READONLY_CANTLOCK -> "SQLITE_READONLY_CANTLOCK"
                SQLITE_READONLY_ROLLBACK -> "SQLITE_READONLY_ROLLBACK"
                SQLITE_READONLY_DBMOVED -> "SQLITE_READONLY_DBMOVED"
                SQLITE_READONLY_CANTINIT -> "SQLITE_READONLY_CANTINIT"
                SQLITE_READONLY_DIRECTORY -> "SQLITE_READONLY_DIRECTORY"
                SQLITE_ABORT_ROLLBACK -> "SQLITE_ABORT_ROLLBACK"
                SQLITE_CONSTRAINT_CHECK -> "SQLITE_CONSTRAINT_CHECK"
                SQLITE_CONSTRAINT_COMMITHOOK -> "SQLITE_CONSTRAINT_COMMITHOOK"
                SQLITE_CONSTRAINT_FOREIGNKEY -> "SQLITE_CONSTRAINT_FOREIGNKEY"
                SQLITE_CONSTRAINT_FUNCTION -> "SQLITE_CONSTRAINT_FUNCTION"
                SQLITE_CONSTRAINT_NOTNULL -> "SQLITE_CONSTRAINT_NOTNULL"
                SQLITE_CONSTRAINT_PRIMARYKEY -> "SQLITE_CONSTRAINT_PRIMARYKEY"
                SQLITE_CONSTRAINT_TRIGGER -> "SQLITE_CONSTRAINT_TRIGGER"
                SQLITE_CONSTRAINT_UNIQUE -> "SQLITE_CONSTRAINT_UNIQUE"
                SQLITE_CONSTRAINT_VTAB -> "SQLITE_CONSTRAINT_VTAB"
                SQLITE_CONSTRAINT_ROWID -> "SQLITE_CONSTRAINT_ROWID"
                SQLITE_CONSTRAINT_PINNED -> "SQLITE_CONSTRAINT_PINNED"
                SQLITE_CONSTRAINT_DATATYPE -> "SQLITE_CONSTRAINT_DATATYPE"
                SQLITE_NOTICE_RECOVER_WAL -> "SQLITE_NOTICE_RECOVER_WAL"
                SQLITE_NOTICE_RECOVER_ROLLBACK -> "SQLITE_NOTICE_RECOVER_ROLLBACK"
                SQLITE_NOTICE_RBU -> "SQLITE_NOTICE_RBU"
                SQLITE_WARNING_AUTOINDEX -> "SQLITE_WARNING_AUTOINDEX"
                SQLITE_AUTH_USER -> "SQLITE_AUTH_USER"
                SQLITE_OK_LOAD_PERMANENTLY -> "SQLITE_OK_LOAD_PERMANENTLY"
                else -> "UNKNOWN($id)"
            }
    }
}

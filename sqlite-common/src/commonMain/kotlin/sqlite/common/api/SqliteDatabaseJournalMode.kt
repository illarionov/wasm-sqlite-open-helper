/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

public enum class SqliteDatabaseJournalMode(public val id: String) {
    /**
     * The [WAL] journaling mode uses a write-ahead log instead of a rollback journal to
     * implement transactions. The WAL journaling mode is persistent; after being set it stays
     * in effect across multiple database connections and after closing and reopening the database.
     *
     * Performance Considerations:
     * This mode is recommended when the goal is to improve write performance or parallel read/write
     * performance. However, it is important to note that WAL introduces checkpoints which commit
     * all transactions that have not been synced to the database thus to maximize read performance
     * and lower checkpointing cost a small journal size is recommended. However, other modes such
     * as [DELETE] will not perform checkpoints, so it is a trade off that needs to be
     * considered as part of the decision of which journal mode to use.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_journal_mode) for more details.
     */
    WAL("WAL"),

    /**
     * The [PERSIST] journaling mode prevents the rollback journal from being deleted at the
     * end of each transaction. Instead, the header of the journal is overwritten with zeros.
     * This will prevent other database connections from rolling the journal back.
     *
     * This mode is useful as an optimization on platforms where deleting or truncating a file is
     * much more expensive than overwriting the first block of a file with zeros.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_journal_mode) for more details.
     */
    PERSIST("PERSIST"),

    /**
     * The [TRUNCATE] journaling mode commits transactions by truncating the rollback journal
     * to zero-length instead of deleting it. On many systems, truncating a file is much faster than
     * deleting the file since the containing directory does not need to be changed.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_journal_mode) for more details.
     */
    TRUNCATE("TRUNCATE"),

    /**
     * The [MEMORY] journaling mode stores the rollback journal in volatile RAM.
     * This saves disk I/O but at the expense of database safety and integrity. If the application
     * using SQLite crashes in the middle of a transaction when the MEMORY journaling mode is set,
     * then the database file will very likely go corrupt.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_journal_mode) for more details.
     */
    MEMORY("MEMORY"),

    /**
     * The [DELETE] journaling mode is the normal behavior. In the DELETE mode, the rollback
     * journal is deleted at the conclusion of each transaction.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_journal_mode) for more details.
     */
    DELETE("DELETE"),

    /**
     * The [OFF] journaling mode disables the rollback journal completely. No rollback journal
     * is ever created and hence there is never a rollback journal to delete. The OFF journaling
     * mode disables the atomic commit and rollback capabilities of SQLite. The ROLLBACK command
     * behaves in an undefined way thus applications must avoid using the ROLLBACK command.
     * If the application crashes in the middle of a transaction, then the database file will very
     * likely go corrupt.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_journal_mode) for more details.
     */
    OFF("OFF"),
}

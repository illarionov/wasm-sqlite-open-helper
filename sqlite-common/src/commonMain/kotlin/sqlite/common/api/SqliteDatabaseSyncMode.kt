/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

public enum class SqliteDatabaseSyncMode(public val id: String) {
    /**
     * The [EXTRA] sync mode is like [FULL] sync mode with the addition that the
     * directory containing a rollback journal is synced after that journal is unlinked to commit a
     * transaction in [DELETE] journal mode.
     *
     * [EXTRA] provides additional durability if the commit is followed closely by a power loss.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_synchronous) for more details.
     */
    EXTRA("EXTRA"),

    /**
     * In [FULL] sync mode the SQLite database engine will use the xSync method of the VFS
     * to ensure that all content is safely written to the disk surface prior to continuing.
     * This ensures that an operating system crash or power failure will not corrupt the database.
     * [FULL] is very safe, but it is also slower.
     *
     * [FULL] is the most commonly used synchronous setting when not in WAL mode.
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_synchronous) for more details.
     */
    FULL("FULL"),

    /**
     * The [NORMAL] sync mode, the SQLite database engine will still sync at the most critical
     * moments, but less often than in [FULL] mode. There is a very small chance that a
     * power failure at the wrong time could corrupt the database in [DELETE] journal mode on
     * an older filesystem.
     *
     * [WAL] journal mode is safe from corruption with [NORMAL] sync mode, and probably
     * [DELETE] sync mode is safe too on modern filesystems. WAL mode is always consistent
     * with [NORMAL] sync mode, but WAL mode does lose durability. A transaction committed in
     * WAL mode with [NORMAL] might roll back following a power loss or system crash.
     * Transactions are durable across application crashes regardless of the synchronous setting
     * or journal mode.
     *
     * The [NORMAL] sync mode is a good choice for most applications running in WAL mode.
     *
     * Caveat: Even though this sync mode is safe Be careful when using [NORMAL] sync mode
     * when dealing with data dependencies between multiple databases, unless those databases use
     * the same durability or are somehow synced, there could be corruption.</p>
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_synchronous) for more details.
     */
    NORMAL("NORMAL"),

    /**
     * In [OFF] sync mode SQLite continues without syncing as soon as it has handed data off
     * to the operating system. If the application running SQLite crashes, the data will be safe,
     * but the database might become corrupted if the operating system crashes or the computer loses
     * power before that data has been written to the disk surface. On the other hand, commits can
     * be orders of magnitude faster with synchronous [OFF].
     *
     * See [here](https://www.sqlite.org/pragma.html#pragma_synchronous) for more details.
     */
    OFF("OFF"),
}

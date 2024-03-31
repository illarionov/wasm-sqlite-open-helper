/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api

import kotlin.jvm.JvmInline

/**
 * SQLite C Interface: Constants Defining Special Destructor Behavior
 *
 * https://www.sqlite.org/c3ref/c_static.html
 */
@JvmInline
public value class SqliteDestructorType(
    public val id: Int,
) {
    public companion object {
        /**
         * The content pointer is constant and will never change. It does not need to be destroyed
         */
        public val SQLITE_STATIC: SqliteDestructorType = SqliteDestructorType(0)

        /**
         * The content will likely change in the near future and SQLite should make its own private copy of the content
         * before returning.
         */
        public val SQLITE_TRANSIENT: SqliteDestructorType = SqliteDestructorType(-1)
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.internal

internal class SqliteDebug(
    val sqlLog: Boolean = false,
    val sqlStatements: Boolean = false,
    val sqlTime: Boolean = false,
    val logSlowQueries: Boolean = false,
    val slowQueryThresholdProvider: () -> Int,
) {
    /**
     * Determines whether a query should be logged.
     */
    internal fun shouldLogSlowQuery(elapsedTimeMillis: Long): Boolean {
        val slowQueryMillis = slowQueryThresholdProvider()
        return slowQueryMillis in 0..elapsedTimeMillis
    }
}

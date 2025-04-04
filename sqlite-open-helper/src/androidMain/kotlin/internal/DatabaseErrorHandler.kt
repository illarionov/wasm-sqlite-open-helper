/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.internal

/**
 * An interface to let apps define an action to take when database corruption is detected.
 */
internal fun interface DatabaseErrorHandler {
    /**
     * The method invoked when database corruption is detected.
     *
     * @param dbObj the [SQLiteDatabase] object representing the database on which corruption
     * is detected.
     */
    fun onCorruption(dbObj: SQLiteDatabase)
}

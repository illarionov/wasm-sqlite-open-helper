/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.dsl.path

public fun interface DatabasePathResolver {
    /**
     * Return an absolute path to the given database as string.
     */
    public fun getDatabasePath(databaseName: String): String
}

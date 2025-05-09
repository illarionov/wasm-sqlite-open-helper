/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.test.base

import androidx.sqlite.db.SupportSQLiteOpenHelper
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.weh.common.api.Logger

interface TestOpenHelperFactoryCreator {
    public val defaultSqliteBinary: WasmSqliteConfiguration
    public fun create(
        dstDir: String,
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration,
    ): SupportSQLiteOpenHelper.Factory
}

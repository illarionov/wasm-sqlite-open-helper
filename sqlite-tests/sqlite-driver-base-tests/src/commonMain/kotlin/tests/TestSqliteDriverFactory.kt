/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.test.base.tests

import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger

public interface TestSqliteDriverFactory<out S : SQLiteDriver> {
    public val defaultSqliteBinary: WasmSqliteConfiguration

    public fun create(
        dbLogger: Logger,
        sqlite3Binary: WasmSqliteConfiguration = defaultSqliteBinary,
    ): S
}

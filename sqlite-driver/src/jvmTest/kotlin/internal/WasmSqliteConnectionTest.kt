/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.internal

import at.released.wasm.sqlite.driver.dsl.OpenParamsBlock
import at.released.wasm.sqlite.open.helper.WasmPtr
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import at.released.wasm.sqlite.open.helper.sqlite.common.api.SqliteResultCode
import at.released.wasm.sqlite.open.helper.sqlite.common.capi.Sqlite3CApi
import at.released.weh.test.logger.TestLogger
import co.touchlab.kermit.Severity.Assert
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class WasmSqliteConnectionTest {
    val logger = TestLogger(minSeverity = Assert)

    @Test
    fun `WasmSqliteConnection should close underlying connection on close() only once`() {
        val connectionPtr: WasmPtr<SqliteDb> = WasmPtr(0x42)
        val cApi: Sqlite3CApi = mockk()
        every { cApi.db.sqlite3Close(connectionPtr) } returns SqliteResultCode.SQLITE_OK

        val connection = WasmSqliteConnection(
            databaseLabel = "testConnection",
            openParams = OpenParamsBlock(),
            connectionPtr = connectionPtr,
            cApi = cApi,
            rootLogger = logger,
        )

        connection.close()
        connection.close()

        verify(exactly = 1) { cApi.db.sqlite3Close(connectionPtr) }
    }
}

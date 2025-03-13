/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.base

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import at.released.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import at.released.wasm.sqlite.driver.test.base.tests.room.UserDatabaseTests.UserDatabaseFactory
import kotlin.coroutines.CoroutineContext

internal object JvmDatabaseFactory : UserDatabaseFactory {
    override fun create(
        driver: SQLiteDriver,
        databaseName: String?,
        queryCoroutineContext: CoroutineContext,
    ): UserDatabaseSuspend {
        val builder = if (databaseName != null) {
            Room.databaseBuilder<UserDatabaseSuspend>(databaseName)
        } else {
            Room.inMemoryDatabaseBuilder<UserDatabaseSuspend>()
        }
        return builder
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setDriver(driver)
            .setQueryCoroutineContext(queryCoroutineContext)
            .build()
    }
}

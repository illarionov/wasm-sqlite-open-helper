/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.base

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import androidx.test.platform.app.InstrumentationRegistry
import ru.pixnews.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractBasicRoomTest.UserDatabaseFactory
import kotlin.coroutines.CoroutineContext

internal object AndroidUserDatabaseSuspendFactory : UserDatabaseFactory {
    override fun create(
        driver: SQLiteDriver,
        databaseName: String?,
        queryCoroutineContext: CoroutineContext,
    ): UserDatabaseSuspend {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val builder = if (databaseName != null) {
            Room.databaseBuilder(
                context,
                UserDatabaseSuspend::class.java,
                databaseName,
            )
        } else {
            Room.inMemoryDatabaseBuilder(
                context,
                UserDatabaseSuspend::class.java,
            )
        }
        return builder
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .setDriver(driver)
            .allowMainThreadQueries()
            .setQueryCoroutineContext(queryCoroutineContext)
            .build()
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.base

import android.content.ContextWrapper
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteDriver
import ru.pixnews.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend
import ru.pixnews.wasm.sqlite.driver.test.base.tests.room.UserDatabaseTests.UserDatabaseFactory
import java.io.File
import kotlin.coroutines.CoroutineContext

internal object AndroidUserDatabaseSuspendFactory : UserDatabaseFactory {
    override fun create(
        driver: SQLiteDriver,
        databaseName: String?,
        queryCoroutineContext: CoroutineContext,
    ): UserDatabaseSuspend {
        val mockContext = object : ContextWrapper(null) {
            override fun getDatabasePath(name: String?): File = File(name!!)
        }
        val builder = if (databaseName != null) {
            Room.databaseBuilder(
                mockContext,
                UserDatabaseSuspend::class.java,
                databaseName,
            )
        } else {
            Room.inMemoryDatabaseBuilder(
                mockContext,
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

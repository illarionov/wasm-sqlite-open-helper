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
import kotlinx.coroutines.test.TestScope
import ru.pixnews.wasm.sqlite.driver.test.base.room.UserDatabaseSuspend

val userDatabaseSuspendFactory: TestScope.(driver: SQLiteDriver) -> UserDatabaseSuspend = { driver ->
    val mockContext = ContextWrapper(null)
    Room.databaseBuilder(
        mockContext,
        UserDatabaseSuspend::class.java,
        "database-name",
    )
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .setDriver(driver)
        .allowMainThreadQueries()
        .setQueryCoroutineContext(this.backgroundScope.coroutineContext)
        .build()
}

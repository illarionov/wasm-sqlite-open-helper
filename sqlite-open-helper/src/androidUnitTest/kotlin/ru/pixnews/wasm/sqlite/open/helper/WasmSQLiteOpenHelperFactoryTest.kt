/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import android.content.ContextWrapper
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import co.touchlab.kermit.Severity.Info
import co.touchlab.kermit.Severity.Verbose
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import ru.pixnews.wasm.sqlite.open.helper.room.AppDatabase1
import ru.pixnews.wasm.sqlite.open.helper.room.User
import ru.pixnews.wasm.sqlite.open.helper.util.KermitLogger
import ru.pixnews.wasm.sqlite.open.helper.util.createWasmSQLiteOpenHelper
import ru.pixnews.wasm.sqlite.open.helper.util.createWasmSqliteOpenHelperFactory
import java.io.File

class WasmSQLiteOpenHelperFactoryTest {
    val logger = KermitLogger("RequerySQLiteOpenHelperFactoryTest")
    val dbLogger = KermitLogger(tag = "WSOH", minSeverity = Verbose)

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `Factory initialization should work`() {
        val helper = createWasmSQLiteOpenHelper(tempDir, dbLogger)
        helper.writableDatabase.use { db: SupportSQLiteDatabase ->
            logger.i { "db: $db; version: ${db.version}" }
            db.execSQL("CREATE TABLE IF NOT EXISTS User(id INTEGER PRIMARY KEY, name TEXT)")
            db.execSQL(
                "INSERT INTO User(`name`) VALUES (?), (?), (?)",
                arrayOf("user 1", "user 2", "user 3"),
            )
            db.query("SELECT * FROM User").use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow("name"))
                    logger.i { "$id: $name" }
                }
            }
        }
    }

    @Test
    fun `Test Room`() {
        val helperFactory = createWasmSqliteOpenHelperFactory(tempDir, dbLogger)
        val mockContext = ContextWrapper(null)
        val db: AppDatabase1 = Room.databaseBuilder(mockContext, AppDatabase1::class.java, "database-name")
            .openHelperFactory(helperFactory)
            .allowMainThreadQueries()
            .build()
        val userDao = db.userDao()

        val user101 = User(101, "User 101 First Name", "User 101 Last Name")
        userDao.insertAll(
            User(100, "User 100 First Name", "User 100 Last Name"),
            user101,
            User(102, "User 102 First Name", "User 102 Last Name"),
        )
        userDao.delete(user101)

        val usersByIds = userDao.loadAllByIds(intArrayOf(101, 102))
        val userByName = userDao.findByName("User 102 First Name", "User 102 Last Name")
        val users: List<User> = userDao.getAll()

        logger.i { "users by ids: $usersByIds; user by name: $userByName; users: $users;" }

        db.close()
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.test.base.tests

import android.content.ContextWrapper
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.wasm.sqlite.open.helper.test.base.AbstractOpenHelperFactoryTest
import at.released.wasm.sqlite.open.helper.test.base.TestOpenHelperFactoryCreator
import at.released.wasm.sqlite.open.helper.test.base.room.User
import at.released.wasm.sqlite.open.helper.test.base.room.UserDatabaseBlocking
import co.touchlab.kermit.Severity
import co.touchlab.kermit.Severity.Info
import java.io.File
import kotlin.test.Test

abstract class AbstractCommonFactoryTest<E : SqliteEmbedderConfig>(
    factoryCreator: TestOpenHelperFactoryCreator,
    dbLoggerSeverity: Severity = Info,
) : AbstractOpenHelperFactoryTest<E>(
    factoryCreator = factoryCreator,
    dbLoggerSeverity = dbLoggerSeverity,
) {
    @Test
    open fun Factory_initialization_should_work() {
        val helper = createWasmSQLiteOpenHelper()
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
    @Suppress("MagicNumber")
    open fun Test_Room() {
        val helperFactory = createWasmSQLiteOpenHelperFactory()
        val mockContext = object : ContextWrapper(null) {
            override fun getDatabasePath(name: String?): File = File(name!!)
        }
        val db: UserDatabaseBlocking = Room.databaseBuilder(
            mockContext,
            UserDatabaseBlocking::class.java,
            "database-name",
        )
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

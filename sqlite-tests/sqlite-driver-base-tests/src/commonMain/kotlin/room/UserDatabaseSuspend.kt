/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.driver.test.base.room

import androidx.room.ColumnInfo
import androidx.room.ConstructedBy
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Entity
public data class User(
    @PrimaryKey val uid: Int,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?,
)

@Dao
public interface UserDao {
    @Query("SELECT * FROM user")
    public suspend fun getAll(): List<User>

    @Query("SELECT * FROM user WHERE uid IN (:userIds)")
    public suspend fun loadAllByIds(userIds: IntArray): List<User>

    @Query(
        "SELECT * FROM user WHERE first_name LIKE :first AND " +
                "last_name LIKE :last LIMIT 1",
    )
    public suspend fun findByName(first: String, last: String): User

    @Insert
    public suspend fun insertAll(vararg users: User)

    @Delete
    public suspend fun delete(user: User)
}

expect object UserDatabaseSuspendConstructor : RoomDatabaseConstructor<UserDatabaseSuspend>

@Database(entities = [User::class], version = 1)
@ConstructedBy(UserDatabaseSuspendConstructor::class)
public abstract class UserDatabaseSuspend : RoomDatabase() {
    public abstract fun userDao(): UserDao
}

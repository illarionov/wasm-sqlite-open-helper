/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.test.base

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import at.released.weh.common.api.Logger

internal class LoggingOpenHelperCallback(
    private val logger: Logger = Logger.withTag("SupportSQLiteOpenHelperCallback"),
    version: Int = 1,
) : SupportSQLiteOpenHelper.Callback(version) {
    override fun onCreate(db: SupportSQLiteDatabase) {
        logger.i { "onCreate(); Database: `$db`" }
    }

    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
        logger.i { "onUpgrade() Database: `$db`, oldVersion: $oldVersion, newVersion: $newVersion" }
    }
}

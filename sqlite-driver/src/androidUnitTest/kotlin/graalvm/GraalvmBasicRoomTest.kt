/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.graalvm

import ru.pixnews.wasm.sqlite.driver.base.userDatabaseSuspendFactory
import ru.pixnews.wasm.sqlite.driver.test.base.tests.AbstractBasicRoomTest
import ru.pixnews.wasm.sqlite.open.helper.chicory.ChicorySqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteEmbedderConfig

class GraalvmBasicRoomTest : AbstractBasicRoomTest<GraalvmSqliteEmbedderConfig>(
    driverCreator = GraalvmSqliteDriverCreator(),
    databaseFactory = userDatabaseSuspendFactory,
)

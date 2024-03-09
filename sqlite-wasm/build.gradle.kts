/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.sqlite.open.helper.wasm.builder")
}

val defaultSqliteVersion = versionCatalogs.named("libs").findVersion("sqlite").get().toString()

sqlite3Build {
    builds {
        create("main") {
            sqliteVersion = defaultSqliteVersion
        }
        create("main2") {
            sqliteVersion = defaultSqliteVersion
        }
    }
}

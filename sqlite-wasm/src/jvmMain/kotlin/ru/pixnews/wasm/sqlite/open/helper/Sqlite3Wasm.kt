/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import java.net.URL

public object Sqlite3Wasm {
    public object Emscripten {
        public val sqlite3_345: URL
            get() = getUrl("sqlite3-main-3450100.wasm")
        public val sqlite3_345_mt: URL
            get() = getUrl("sqlite3-main-mt-3450100.wasm")
        public val sqlite3_345_mt_pthread: URL
            get() = getUrl("sqlite3-main-mt-3450100.wasm")
    }

    private fun getUrl(fileName: String): URL = requireNotNull(Sqlite3Wasm::class.java.getResource(fileName))
}

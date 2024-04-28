/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import java.net.URL

//  Temporary build with unstripped symbols for debugging
public object SqliteAndroidWasmEmscriptenIcuUnstripped345 : WasmSqliteConfiguration {
    override val sqliteUrl: URL
        get() = requireNotNull(
            SqliteAndroidWasmEmscriptenIcu345::class.java.getResource(
                "sqlite3-android-icu-3450200-unstripped.wasm",
            ),
        )
    override val wasmMinMemorySize: Long = 50_331_648L
    override val requireThreads: Boolean = false
    override val requireSharedMemory: Boolean = false
}

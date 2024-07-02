/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.io

import okio.FileSystem
import okio.Path.Companion.toPath
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public actual fun getBinaryReader(): WasmBinaryReader = NativeBinaryReader

private object NativeBinaryReader : WasmBinaryReader {
    override fun readBytes(url: String): ByteArray {
        val path = url.toPath()
        return FileSystem.SYSTEM.read(path) {
            this.readByteArray()
        }
    }
}

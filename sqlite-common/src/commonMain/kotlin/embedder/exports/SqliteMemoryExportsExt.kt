/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.exports

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.writeNullTerminatedString

@InternalWasmSqliteHelperApi
public fun SqliteDynamicMemoryExports.allocNullTerminatedString(
    memory: EmbedderMemory,
    string: String,
): WasmPtr<Byte> {
    val bytes = string.encodeToByteArray()
    val mem: WasmPtr<Byte> = sqliteAllocOrThrow(bytes.size.toUInt() + 1U)
    memory.writeNullTerminatedString(mem, string)
    return mem
}

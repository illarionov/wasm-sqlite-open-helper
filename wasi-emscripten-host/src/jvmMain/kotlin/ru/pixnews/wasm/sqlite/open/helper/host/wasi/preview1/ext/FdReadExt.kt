/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.memory.readPtr
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Iovec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Size

@Suppress("VARIABLE_HAS_PREFIX")
public object FdReadExt {
    public fun readIovecs(
        memory: Memory,
        pIov: WasmPtr<Iovec>,
        iovCnt: Int,
    ): IovecArray {
        @Suppress("UNCHECKED_CAST", "MagicNumber")
        val iovecs = MutableList(iovCnt) { idx ->
            val pIovec: WasmPtr<*> = pIov + 8 * idx
            Iovec(
                buf = memory.readPtr(pIovec as WasmPtr<WasmPtr<Byte>>),
                bufLen = Size(memory.readI32(pIovec + 4).toUInt()),
            )
        }
        return IovecArray(iovecs)
    }
}

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
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Size

public object FdWriteExt {
    @Suppress("UNCHECKED_CAST")
    public fun readCiovecs(
        memory: Memory,
        pCiov: WasmPtr<CioVec>,
        ciovCnt: Int,
    ): CiovecArray {
        val iovecs = MutableList(ciovCnt) { idx ->
            val pCiovec: WasmPtr<*> = pCiov + 8 * idx
            CioVec(
                buf = memory.readPtr(pCiovec as WasmPtr<WasmPtr<Byte>>),
                bufLen = Size(memory.readI32(pCiovec + 4).toUInt()),
            )
        }
        return CiovecArray(iovecs)
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.memory

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.EmbedderMemory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray

public interface Memory : EmbedderMemory {
    public fun readFromChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong

    public fun writeToChannel(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray,
    ): ULong
}

public fun Memory.readU8(addr: WasmPtr<*>): UByte = readI8(addr).toUByte()
public fun Memory.readU32(addr: WasmPtr<*>): UInt = readI32(addr).toUInt()

@Suppress("UNCHECKED_CAST")
public fun <T : Any, P : WasmPtr<T>> Memory.readPtr(addr: WasmPtr<P>): P = WasmPtr<T>(readI32(addr)) as P
public fun Memory.writePtr(addr: WasmPtr<*>, data: WasmPtr<*>): Unit = writeI32(addr, data.addr)

public fun Memory.write(addr: WasmPtr<*>, data: ByteArray): Unit = write(addr, data, 0, data.size)

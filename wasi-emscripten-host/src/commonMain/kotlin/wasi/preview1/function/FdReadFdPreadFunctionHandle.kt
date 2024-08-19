/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.ReadOnlyMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.plus
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Iovec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Size

public class FdReadFdPreadFunctionHandle private constructor(
    host: EmbedderHost,
    function: HostFunction,
    private val strategy: ReadWriteStrategy,
) : HostFunctionHandle(function, host) {
    public fun execute(
        memory: Memory,
        bulkReader: WasiMemoryReader,
        fd: Fd,
        pIov: WasmPtr<Iovec>,
        iovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Errno {
        val ioVecs: IovecArray = readIovecs(memory, pIov, iovCnt)
        return bulkReader.read(fd, strategy, ioVecs)
            .onRight { readBytes -> memory.writeI32(pNum, readBytes.toInt()) }
            .fold(
                ifLeft = FileSystemOperationError::errno,
                ifRight = { Errno.SUCCESS },
            )
    }

    public companion object {
        public fun fdRead(
            host: EmbedderHost,
        ): FdReadFdPreadFunctionHandle = FdReadFdPreadFunctionHandle(
            host,
            WasiHostFunction.FD_READ,
            ReadWriteStrategy.CHANGE_POSITION,
        )

        public fun fdPread(
            host: EmbedderHost,
        ): FdReadFdPreadFunctionHandle = FdReadFdPreadFunctionHandle(
            host,
            WasiHostFunction.FD_PREAD,
            ReadWriteStrategy.DO_NOT_CHANGE_POSITION,
        )

        private fun readIovecs(
            memory: ReadOnlyMemory,
            pIov: WasmPtr<Iovec>,
            iovCnt: Int,
        ): IovecArray {
            @Suppress("UNCHECKED_CAST")
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
}

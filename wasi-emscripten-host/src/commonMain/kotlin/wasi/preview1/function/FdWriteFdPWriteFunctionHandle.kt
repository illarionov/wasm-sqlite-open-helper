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
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.plus
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readwrite.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Size

public class FdWriteFdPWriteFunctionHandle private constructor(
    host: EmbedderHost,
    function: HostFunction,
    private val strategy: ReadWriteStrategy,
) : HostFunctionHandle(function, host) {
    public fun execute(
        memory: Memory,
        bulkWriter: WasiMemoryWriter,
        fd: Fd,
        pCiov: WasmPtr<CioVec>,
        cIovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Errno {
        val cioVecs: CiovecArray = readCiovecs(memory, pCiov, cIovCnt)
        return bulkWriter.write(fd, strategy, cioVecs)
            .onRight { writtenBytes ->
                memory.writeI32(pNum, writtenBytes.toInt())
            }.fold(
                ifLeft = FileSystemOperationError::errno,
                ifRight = { Errno.SUCCESS },
            )
    }

    public companion object {
        public fun fdWrite(
            host: EmbedderHost,
        ): FdWriteFdPWriteFunctionHandle = FdWriteFdPWriteFunctionHandle(
            host,
            WasiHostFunction.FD_WRITE,
            ReadWriteStrategy.CHANGE_POSITION,
        )

        public fun fdPwrite(
            host: EmbedderHost,
        ): FdWriteFdPWriteFunctionHandle = FdWriteFdPWriteFunctionHandle(
            host,
            WasiHostFunction.FD_PWRITE,
            ReadWriteStrategy.DO_NOT_CHANGE_POSITION,
        )

        @Suppress("UNCHECKED_CAST")
        private fun readCiovecs(
            memory: ReadOnlyMemory,
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
}

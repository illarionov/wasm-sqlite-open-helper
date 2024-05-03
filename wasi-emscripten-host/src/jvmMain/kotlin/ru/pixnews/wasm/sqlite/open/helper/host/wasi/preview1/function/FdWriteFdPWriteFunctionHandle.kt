/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.FdWriteExt.readCiovecs
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CioVec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

public class FdWriteFdPWriteFunctionHandle private constructor(
    host: SqliteEmbedderHost,
    function: HostFunction,
    private val strategy: ReadWriteStrategy,
) : HostFunctionHandle(function, host) {
    public fun execute(
        memory: Memory,
        fd: Fd,
        pCiov: WasmPtr<CioVec>,
        cIovCnt: Int,
        pNum: WasmPtr<Int>,
    ): Errno {
        val cioVecs: CiovecArray = readCiovecs(memory, pCiov, cIovCnt)
        return try {
            val channel = host.fileSystem.getStreamByFd(fd)
            val writtenBytes = memory.writeToChannel(channel, strategy, cioVecs)
            memory.writeI32(pNum, writtenBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.i(e) { "write() error" }
            e.errNo
        }
    }

    public companion object {
        public fun fdWrite(
            host: SqliteEmbedderHost,
        ): FdWriteFdPWriteFunctionHandle = FdWriteFdPWriteFunctionHandle(
            host,
            WasiHostFunction.FD_WRITE,
            ReadWriteStrategy.CHANGE_POSITION,
        )

        public fun fdPwrite(
            host: SqliteEmbedderHost,
        ): FdWriteFdPWriteFunctionHandle = FdWriteFdPWriteFunctionHandle(
            host,
            WasiHostFunction.FD_PWRITE,
            ReadWriteStrategy.DO_NOT_CHANGE_POSITION,
        )
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.wasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I64
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.position
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence
import java.util.logging.Level
import java.util.logging.Logger

internal fun fdSeek(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "fd_seek",
    paramTypes = listOf(
        Fd.wasmValueType, // fd
        I64, // offset
        I32, // whence
        I64.pointer, // *newOffset
    ),
    moduleName = moduleName,
    handle = FdSeek(memory, filesystem),
)

private class FdSeek(
    private val memory: Memory,
    private val filesystem: FileSystem,
    private val logger: Logger = Logger.getLogger(FdSeek::class.qualifiedName),
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val offset = args[1].asLong()
        val whence = Whence.fromIdOrNull(args[2].asInt()) ?: return Errno.INVAL
        val pNewOffset: WasmPtr<Long> = args[3].asWasmAddr()
        return fdSeek(fd, offset, whence, pNewOffset)
    }

    private fun fdSeek(
        fd: Fd,
        offset: Long,
        whence: Whence,
        pNewOffset: WasmPtr<Long>,
    ): Errno {
        return try {
            val channel: FdChannel = filesystem.getStreamByFd(fd)
            filesystem.seek(channel, offset, whence)

            val newPosition = channel.position

            memory.writeI64(pNewOffset, newPosition)

            Errno.SUCCESS
        } catch (sysException: SysException) {
            logger.log(Level.INFO, sysException) { "fdSeek() error" }
            sysException.errNo
        }
    }
}

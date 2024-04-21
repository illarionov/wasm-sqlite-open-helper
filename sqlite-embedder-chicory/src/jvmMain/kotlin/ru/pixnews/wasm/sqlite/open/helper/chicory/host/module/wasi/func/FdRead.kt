/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.FdReadExt.readIovecs
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Iovec
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.util.logging.Level
import java.util.logging.Logger

internal fun fdRead(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdRead(memory, filesystem, moduleName, "fd_read", CHANGE_POSITION)

internal fun fdPread(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = fdRead(memory, filesystem, moduleName, "fd_pread", DO_NOT_CHANGE_POSITION)

private fun fdRead(
    memory: Memory,
    filesystem: FileSystem,
    moduleName: String,
    fieldName: String,
    strategy: ReadWriteStrategy,
): HostFunction = wasiHostFunction(
    funcName = fieldName,
    paramTypes = listOf(
        Fd.wasmValueType, // Fd
        IovecArray.pointer, // iov
        I32, // iov_cnt
        I32.pointer, // pNum
    ),
    moduleName = moduleName,
    handle = FdRead(memory, filesystem, strategy),
)

private class FdRead(
    private val memory: Memory,
    private val filesystem: FileSystem,
    private val strategy: ReadWriteStrategy,
    private val logger: Logger = Logger.getLogger(FdRead::class.qualifiedName),
) : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        val fd = Fd(args[0].asInt())
        val pIov: WasmPtr<Iovec> = args[1].asWasmAddr()
        val iovCnt = args[2].asInt()
        val pNum: WasmPtr<Int> = args[3].asWasmAddr()

        val ioVecs = readIovecs(memory, pIov, iovCnt)
        return try {
            val channel = filesystem.getStreamByFd(fd)
            val readBytes = memory.readFromChannel(channel, strategy, ioVecs)
            memory.writeI32(pNum, readBytes.toInt())
            Errno.SUCCESS
        } catch (e: SysException) {
            logger.log(Level.INFO, e) { "read() error" }
            e.errNo
        }
    }
}

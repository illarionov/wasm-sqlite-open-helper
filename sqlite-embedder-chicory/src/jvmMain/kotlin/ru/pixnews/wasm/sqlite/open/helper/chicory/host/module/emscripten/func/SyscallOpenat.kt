/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("MemberNameEqualsClassName")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readZeroTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.resolveAbsolutePath
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.oMaskToString
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8
import java.nio.file.Path

internal fun syscallOpenat(
    memory: Memory,
    filesystem: FileSystem,
    logger: Logger,
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "__syscall_openat",
    paramTypes = listOf(
        I32, // dirfd
        U8.pointer, // pathname
        I32, // flags
        I32, // mode / varargs
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = Openat(memory, filesystem, logger),
)

private class Openat(
    private val memory: Memory,
    private val filesystem: FileSystem,
    private val logger: ru.pixnews.wasm.sqlite.open.helper.common.api.Logger,
) : EmscriptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val mode = if (args.lastIndex == 3) {
            instance.memory().readI32(args[3].asInt()).asInt().toUInt()
        } else {
            0U
        }

        val fdOrErrno = openAt(
            rawDirFd = args[0].asInt(),
            pathnamePtr = args[1].asWasmAddr(),
            flags = args[2].asInt().toUInt(),
            rawMode = mode,
        )
        return Value.i32(fdOrErrno.toLong())
    }

    private fun openAt(
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        flags: UInt,
        rawMode: UInt,
    ): Int {
        val dirFd = DirFd(rawDirFd)
        val mode = FileMode(rawMode)
        val path = memory.readZeroTerminatedString(pathnamePtr)
        val absolutePath = filesystem.resolveAbsolutePath(dirFd, path)

        return try {
            val fd = filesystem.open(absolutePath, flags, mode).fd
            logger.v { formatCallString(dirFd, path, absolutePath, flags, mode, fd) }
            fd.fd
        } catch (e: SysException) {
            logger.v {
                formatCallString(dirFd, path, absolutePath, flags, mode, null) +
                        "openAt() error ${e.errNo}"
            }
            -e.errNo.code
        }
    }

    @Suppress("MagicNumber")
    private fun formatCallString(
        dirfd: DirFd,
        path: String,
        absolutePath: Path,
        flags: UInt,
        mode: FileMode,
        fd: Fd?,
    ): String = "openAt() dirfd: " +
            "$dirfd, " +
            "path: `$path`, " +
            "full path: `$absolutePath`, " +
            "flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}), " +
            "mode: $mode" +
            if (fd != null) ": $fd" else ""
}

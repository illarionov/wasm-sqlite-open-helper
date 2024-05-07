/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.common.api.plus
import ru.pixnews.wasm.sqlite.open.helper.common.embedder.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.UTIME_NOW
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.UTIME_OMIT
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

public class SyscallUtimensatFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_UTIMENSAT, host) {
    public fun execute(
        memory: Memory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        times: WasmPtr<Byte>,
        flags: UInt,
    ): Int {
        val dirFd = DirFd(rawDirFd)
        val noFolowSymlinks: Boolean = (flags and Fcntl.AT_SYMLINK_NOFOLLOW) != 0U
        val path = memory.readNullTerminatedString(pathnamePtr)
        var atime: Duration?
        val mtime: Duration?
        @Suppress("MagicNumber")
        if (times.isSqlite3Null()) {
            atime = host.clock.invoke()
            mtime = atime
        } else {
            val atimeSeconds = memory.readI64(times)
            val atimeNanoseconds = memory.readI64(times + 8)

            val mtimeSeconds = memory.readI64(times + 16)
            val mtimeNanoseconds = memory.readI64(times + 24)

            val now: Duration by lazy(NONE) { host.clock.invoke() }
            atime = timesToDuration(atimeSeconds, atimeNanoseconds) { now }
            mtime = timesToDuration(mtimeSeconds, mtimeNanoseconds) { now }
        }
        try {
            host.fileSystem.setTimesAt(dirFd, path, atime, mtime, noFolowSymlinks)
            return 0
        } catch (e: SysException) {
            return -e.errNo.code
        }
    }

    private fun timesToDuration(
        seconds: Long,
        nanoseconds: Long,
        now: () -> Duration,
    ): Duration? = when (nanoseconds) {
        UTIME_NOW.toLong() -> now()
        UTIME_OMIT.toLong() -> null
        else -> seconds.seconds + nanoseconds.nanoseconds
    }
}

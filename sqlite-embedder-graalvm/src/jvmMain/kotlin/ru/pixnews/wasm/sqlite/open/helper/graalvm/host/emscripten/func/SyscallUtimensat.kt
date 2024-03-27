/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.common.api.isSqlite3Null
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsUint
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsWasmPtr
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.UTIME_NOW
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.SysStat.UTIME_OMIT
import kotlin.LazyThreadSafetyMode.NONE
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

internal class SyscallUtimensat(
    language: WasmLanguage,
    module: WasmModule,
    private val host: SqliteEmbedderHost,
    functionName: String = "__syscall_utimensat",
) : BaseWasmNode(language, module, functionName) {
    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        val args: Array<Any> = frame.arguments
        @Suppress("MagicNumber")
        return syscallUtimensat(
            memory(frame),
            args.getArgAsInt(0),
            args.getArgAsWasmPtr(1),
            args.getArgAsWasmPtr(2),
            args.getArgAsUint(3),
        )
    }

    @Suppress("MemberNameEqualsClassName")
    @TruffleBoundary
    private fun syscallUtimensat(
        memory: WasmMemory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        times: WasmPtr<Byte>,
        flags: UInt,
    ): Int {
        val dirFd = DirFd(rawDirFd)
        val noFolowSymlinks: Boolean = (flags and Fcntl.AT_SYMLINK_NOFOLLOW) != 0U
        val path = memory.readString(pathnamePtr.addr, null)
        var atime: Duration?
        val mtime: Duration?
        @Suppress("MagicNumber")
        if (times.isSqlite3Null()) {
            atime = host.clock.invoke()
            mtime = atime
        } else {
            val atimeSeconds = memory.load_i64(this, times.addr.toLong())
            val atimeNanoseconds = memory.load_i64(this, times.addr.toLong() + 8)

            val mtimeSeconds = memory.load_i64(this, times.addr.toLong() + 16)
            val mtimeNanoseconds = memory.load_i64(this, times.addr.toLong() + 24)

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

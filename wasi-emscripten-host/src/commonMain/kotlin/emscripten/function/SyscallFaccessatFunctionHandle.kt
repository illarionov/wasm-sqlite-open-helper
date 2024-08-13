/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.ReadOnlyMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.readNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.ext.negativeErrnoCode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.CheckAccess
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl.AT_EACCESS
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl.AT_SYMLINK_NOFOLLOW
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileAccessibilityCheck

public class SyscallFaccessatFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.SYSCALL_FACCESSAT, host) {
    public fun execute(
        memory: ReadOnlyMemory,
        rawDirFd: Int,
        pathnamePtr: WasmPtr<Byte>,
        amode: UInt,
        flags: UInt,
    ): Int {
        val path = memory.readNullTerminatedString(pathnamePtr)
        return host.fileSystem.execute(
            CheckAccess,
            CheckAccess(
                path = path,
                baseDirectory = BaseDirectory.fromRawDirFd(rawDirFd),
                mode = FileAccessibilityCheck(amode),
                useEffectiveUserId = flags and AT_EACCESS == AT_EACCESS,
                allowEmptyPath = false,
                followSymlinks = flags and AT_SYMLINK_NOFOLLOW != AT_SYMLINK_NOFOLLOW,
            ),
        ).negativeErrnoCode()
    }
}

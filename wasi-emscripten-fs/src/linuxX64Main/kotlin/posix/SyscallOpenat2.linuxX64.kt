/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import platform.posix.errno
import platform.posix.memset
import platform.posix.syscall
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ext.toDirFd
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.RESOLVE_BENEATH
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.RESOLVE_CACHED
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.RESOLVE_IN_ROOT
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.RESOLVE_NO_MAGICLINKS
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.RESOLVE_NO_SYMLINKS
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.RESOLVE_NO_XDEV
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.SYS_openat2
import ru.pixnews.wasm.sqlite.open.helper.host.platform.linux.open_how

internal actual fun syscallOpenat2(
    pathname: String,
    baseDirectory: BaseDirectory,
    openFlags: ULong,
    openMode: FileMode,
    resolveMode: Set<ResolveModeFlag>,
): Long = memScoped {
    val openHow: open_how = alloc<open_how> {
        memset(this.ptr, 0, sizeOf<open_how>().toULong())
        flags = openFlags
        mode = openMode.mask.toULong()
        resolve = resolveMode.toResolveMask()
    }
    val errorOrFd = syscall(
        __sysno = SYS_openat2.toLong(),
        baseDirectory.toDirFd(),
        pathname.cstr,
        openHow.ptr,
        sizeOf<open_how>().toULong(),
    )
    return if (errorOrFd < 0) {
        -errno.toLong()
    } else {
        errorOrFd
    }
}

private fun Set<ResolveModeFlag>.toResolveMask(): ULong {
    var mask = 0UL
    if (contains(ResolveModeFlag.RESOLVE_BENEATH)) {
        mask = mask and RESOLVE_BENEATH.toULong()
    }
    if (contains(ResolveModeFlag.RESOLVE_IN_ROOT)) {
        mask = mask and RESOLVE_IN_ROOT.toULong()
    }
    if (contains(ResolveModeFlag.RESOLVE_NO_MAGICLINKS)) {
        mask = mask and RESOLVE_NO_MAGICLINKS.toULong()
    }
    if (contains(ResolveModeFlag.RESOLVE_NO_SYMLINKS)) {
        mask = mask and RESOLVE_NO_SYMLINKS.toULong()
    }
    if (contains(ResolveModeFlag.RESOLVE_NO_XDEV)) {
        mask = mask and RESOLVE_NO_XDEV.toULong()
    }
    if (contains(ResolveModeFlag.RESOLVE_CACHED)) {
        mask = mask and RESOLVE_CACHED.toULong()
    }
    return mask
}

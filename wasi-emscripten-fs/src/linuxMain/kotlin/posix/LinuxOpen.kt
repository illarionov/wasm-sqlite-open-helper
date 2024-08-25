/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import platform.posix.E2BIG
import platform.posix.EAGAIN
import platform.posix.EINVAL
import platform.posix.ELOOP
import platform.posix.EXDEV
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Again
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotCapable
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.OpenError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.TooManySymbolicLinks
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.OpenFileFlags
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.OpenFileFlags.OpenFileFlag
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.OpenFileFlags.OpenFileFlag.O_ACCMODE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.ResolveModeFlag.RESOLVE_NO_MAGICLINKS
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixFileSystemState
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.posix.base.PosixOperationHandler

internal class LinuxOpen(
    private val state: PosixFileSystemState,
) : PosixOperationHandler<Open, OpenError, Fd> {
    override fun invoke(input: Open): Either<OpenError, Fd> {
        val fdOrErrno = syscallOpenat2(
            baseDirectory = input.baseDirectory,
            pathname = input.path,
            openFlags = input.flags.toLinuxMask(),
            openMode = input.mode,
            resolveMode = setOf(RESOLVE_NO_MAGICLINKS),
        )
        return if (fdOrErrno >= 0) {
            val fd = Fd(fdOrErrno.toInt())
            state.add(fd)
            fd.right()
        } else {
            (-fdOrErrno.toInt()).errNoToOpenError().left()
        }
    }
}

private val openFileFlagsMaskToPosixMask = listOf(
    OpenFileFlag.O_CREAT to platform.posix.O_CREAT,
    OpenFileFlag.O_EXCL to platform.posix.O_EXCL,
    OpenFileFlag.O_NOCTTY to platform.posix.O_NOCTTY,
    OpenFileFlag.O_TRUNC to platform.posix.O_TRUNC,
    OpenFileFlag.O_APPEND to platform.posix.O_APPEND,
    OpenFileFlag.O_NONBLOCK to platform.posix.O_NONBLOCK,
    OpenFileFlag.O_NDELAY to platform.posix.O_NDELAY,
    OpenFileFlag.O_DSYNC to platform.posix.O_DSYNC,
    OpenFileFlag.O_ASYNC to platform.posix.O_ASYNC,
    OpenFileFlag.O_DIRECTORY to platform.posix.O_DIRECTORY,
    OpenFileFlag.O_NOFOLLOW to platform.posix.O_NOFOLLOW,
    OpenFileFlag.O_CLOEXEC to platform.posix.O_CLOEXEC,
    OpenFileFlag.O_SYNC to platform.posix.O_SYNC,
)

@Suppress("CyclomaticComplexMethod")
internal fun OpenFileFlags.toLinuxMask(): ULong {
    val openFlagsMask: UInt = this.mask

    var mask = when (val mode = openFlagsMask and O_ACCMODE) {
        OpenFileFlag.O_RDONLY -> platform.posix.O_RDONLY
        OpenFileFlag.O_WRONLY -> platform.posix.O_WRONLY
        OpenFileFlag.O_RDWR -> platform.posix.O_RDWR
        else -> error("Unknown mode $mode")
    }

    openFileFlagsMaskToPosixMask.forEach { (testMask, posixMask) ->
        if (openFlagsMask and testMask == testMask) {
            mask = mask or posixMask
        }
    }
    // O_DIRECT, O_LARGEFILE, O_NOATIME: Not supported
    // O_PATH, O_TMPFILE: not supported, should be added?

    return mask.toULong()
}

private fun Int.errNoToOpenError(): OpenError = when (this) {
    E2BIG -> InvalidArgument("E2BIG: extension is not supported")
    EAGAIN -> Again("operation cannot be performed")
    EINVAL -> InvalidArgument("Invalid argument")
    ELOOP -> TooManySymbolicLinks("Too many symbolic or magic links")
    EXDEV -> NotCapable("Escape from the root detected")
    else -> InvalidArgument("Unknown errno $this")
}

internal expect fun syscallOpenat2(
    pathname: String,
    baseDirectory: BaseDirectory,
    openFlags: ULong,
    openMode: FileMode,
    resolveMode: Set<ResolveModeFlag>,
): Long

internal enum class ResolveModeFlag {
    RESOLVE_BENEATH,
    RESOLVE_IN_ROOT,
    RESOLVE_NO_MAGICLINKS,
    RESOLVE_NO_SYMLINKS,
    RESOLVE_NO_XDEV,
    RESOLVE_CACHED,
}

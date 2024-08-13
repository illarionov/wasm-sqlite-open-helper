/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.raise.either
import com.sun.nio.file.ExtendedOpenOption
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError.BadFileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError.Exists
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.OpenError.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.EmptyPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.FileDescriptorNotOpen
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.InvalidPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.RelativePath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.asFileAttribute
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.resolvePath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.toPosixFilePermissions
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.file.FileAlreadyExistsException
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.concurrent.withLock

internal class NioOpen(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<Open, OpenError, Fd> {
    override fun invoke(input: Open): Either<OpenError, Fd> = either {
        val path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toOpenError() }
            .bind()

        val openOptionsResult = getOpenOptions(input.flags)
        if (openOptionsResult.notImplementedFlags != 0U) {
            raise(
                InvalidArgument(
                    "Flags 0x${openOptionsResult.notImplementedFlags.toString(16)} not implemented",
                ),
            )
        }
        val fileAttributes = input.mode.toPosixFilePermissions().asFileAttribute()
        fsState.fsLock.withLock {
            val nioChannel = Either.catch {
                FileChannel.open(path, openOptionsResult.options, fileAttributes)
            }
                .mapLeft { error -> error.toOpenError(path) }
                .bind()
            val fdChannel = fsState.fileDescriptors.add(path, nioChannel)
                .mapLeft { noFileDescriptorError -> OpenError.NFile(noFileDescriptorError.message) }
                .bind()
            fdChannel.fd
        }
    }
}

private fun Throwable.toOpenError(path: Path): OpenError = when (this) {
    is IllegalArgumentException -> InvalidArgument(
        "Can not open `$path`: invalid combination of options ($message)",
    )

    is UnsupportedOperationException -> InvalidArgument("Can not open `$path`: unsupported operation ($message)")
    is FileAlreadyExistsException -> Exists("File `$path` already exists ($message)")
    is IOException -> IoError("Can not open `$path`: I/O error ($message)")
    is SecurityException -> PermissionDenied("Can not open `$path`: Permission denied ($message)")
    else -> throw IllegalStateException("Unexpected error", this)
}

private fun ResolvePathError.toOpenError(): OpenError = when (this) {
    is EmptyPath -> NoEntry(this.message)
    is FileDescriptorNotOpen -> BadFileDescriptor(this.message)
    is NotDirectory -> OpenError.NotDirectory(this.message)
    is InvalidPath, is RelativePath -> InvalidArgument(this.message)
}

@Suppress("CyclomaticComplexMethod", "LOCAL_VARIABLE_EARLY_DECLARATION", "LongMethod")
private fun getOpenOptions(
    flags: UInt,
): GetOpenOptionsResult {
    val options: MutableSet<OpenOption> = mutableSetOf()
    var ignoredFlags = 0U
    var notImplementedFlags = 0U

    if (flags and Fcntl.O_WRONLY != 0U) {
        options += StandardOpenOption.WRITE
    } else if (flags and Fcntl.O_RDWR != 0U) {
        options += StandardOpenOption.READ
        options += StandardOpenOption.WRITE
    }

    if (flags and Fcntl.O_APPEND != 0U) {
        options += StandardOpenOption.APPEND
    }

    if (flags and Fcntl.O_CREAT != 0U) {
        options += if (flags and Fcntl.O_EXCL != 0U) {
            StandardOpenOption.CREATE_NEW
        } else {
            StandardOpenOption.CREATE
        }
    }

    if (flags and Fcntl.O_TRUNC != 0U) {
        options += StandardOpenOption.TRUNCATE_EXISTING
    }

    if (flags and Fcntl.O_NONBLOCK != 0U) {
        notImplementedFlags = notImplementedFlags and Fcntl.O_NONBLOCK
    }

    if (flags and Fcntl.O_ASYNC != 0U) {
        notImplementedFlags = notImplementedFlags and Fcntl.O_ASYNC
    }

    if (flags and (Fcntl.O_DSYNC or Fcntl.O_SYNC) != 0U) {
        options += StandardOpenOption.SYNC
    }

    if (flags and Fcntl.O_DIRECT != 0U) {
        options += ExtendedOpenOption.DIRECT
    }

    if (flags and Fcntl.O_DIRECTORY != 0U) {
        notImplementedFlags = notImplementedFlags and Fcntl.O_DIRECTORY
    }

    if (flags and Fcntl.O_NOFOLLOW != 0U) {
        options += LinkOption.NOFOLLOW_LINKS
    }
    if (flags and Fcntl.O_NOATIME != 0U) {
        ignoredFlags = ignoredFlags and Fcntl.O_NOATIME
    }
    if (flags and Fcntl.O_CLOEXEC != 0U) {
        ignoredFlags = ignoredFlags and Fcntl.O_CLOEXEC
    }

    if (flags and Fcntl.O_PATH != 0U) {
        notImplementedFlags = notImplementedFlags and Fcntl.O_PATH
    }

    if (flags and Fcntl.O_TMPFILE != 0U) {
        ignoredFlags = ignoredFlags and Fcntl.O_TMPFILE
        options += StandardOpenOption.DELETE_ON_CLOSE
    }

    return GetOpenOptionsResult(options, ignoredFlags, notImplementedFlags)
}

private class GetOpenOptionsResult(
    val options: Set<OpenOption>,
    val ignoredFlags: UInt,
    val notImplementedFlags: UInt,
)

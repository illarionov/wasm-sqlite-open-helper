/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.raise.either
import com.sun.nio.file.ExtendedOpenOption
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Exists
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.Nfile
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.OpenError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.PermissionDenied
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.asFileAttribute
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.toPosixFilePermissions
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.toCommonError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.OpenFileFlags
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.OpenFileFlags.OpenFileFlag
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
        val path = fsState.pathResolver.resolve(input.path, input.baseDirectory, false)
            .mapLeft(ResolvePathError::toCommonError)
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
                .mapLeft { noFileDescriptorError -> Nfile(noFileDescriptorError.message) }
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

@Suppress("CyclomaticComplexMethod", "LOCAL_VARIABLE_EARLY_DECLARATION", "LongMethod")
private fun getOpenOptions(
    flags: OpenFileFlags,
): GetOpenOptionsResult {
    val options: MutableSet<OpenOption> = mutableSetOf()
    var ignoredFlags = 0U
    var notImplementedFlags = 0U

    if (flags.mask and OpenFileFlag.O_WRONLY != 0U) {
        options += StandardOpenOption.WRITE
    } else if (flags.mask and OpenFileFlag.O_RDWR != 0U) {
        options += StandardOpenOption.READ
        options += StandardOpenOption.WRITE
    }

    if (flags.mask and OpenFileFlag.O_APPEND != 0U) {
        options += StandardOpenOption.APPEND
    }

    if (flags.mask and OpenFileFlag.O_CREAT != 0U) {
        options += if (flags.mask and OpenFileFlag.O_EXCL != 0U) {
            StandardOpenOption.CREATE_NEW
        } else {
            StandardOpenOption.CREATE
        }
    }

    if (flags.mask and OpenFileFlag.O_TRUNC != 0U) {
        options += StandardOpenOption.TRUNCATE_EXISTING
    }

    if (flags.mask and OpenFileFlag.O_NONBLOCK != 0U) {
        notImplementedFlags = notImplementedFlags and OpenFileFlag.O_NONBLOCK
    }

    if (flags.mask and OpenFileFlag.O_ASYNC != 0U) {
        notImplementedFlags = notImplementedFlags and OpenFileFlag.O_ASYNC
    }

    if (flags.mask and (OpenFileFlag.O_DSYNC or OpenFileFlag.O_SYNC) != 0U) {
        options += StandardOpenOption.SYNC
    }

    if (flags.mask and OpenFileFlag.O_DIRECT != 0U) {
        options += ExtendedOpenOption.DIRECT
    }

    if (flags.mask and OpenFileFlag.O_DIRECTORY != 0U) {
        notImplementedFlags = notImplementedFlags and OpenFileFlag.O_DIRECTORY
    }

    if (flags.mask and OpenFileFlag.O_NOFOLLOW != 0U) {
        options += LinkOption.NOFOLLOW_LINKS
    }
    if (flags.mask and OpenFileFlag.O_NOATIME != 0U) {
        ignoredFlags = ignoredFlags and OpenFileFlag.O_NOATIME
    }
    if (flags.mask and OpenFileFlag.O_CLOEXEC != 0U) {
        ignoredFlags = ignoredFlags and OpenFileFlag.O_CLOEXEC
    }

    if (flags.mask and OpenFileFlag.O_PATH != 0U) {
        notImplementedFlags = notImplementedFlags and OpenFileFlag.O_PATH
    }

    if (flags.mask and OpenFileFlag.O_TMPFILE != 0U) {
        ignoredFlags = ignoredFlags and OpenFileFlag.O_TMPFILE
        options += StandardOpenOption.DELETE_ON_CLOSE
    }

    return GetOpenOptionsResult(options, ignoredFlags, notImplementedFlags)
}

private class GetOpenOptionsResult(
    val options: Set<OpenOption>,
    val ignoredFlags: UInt,
    val notImplementedFlags: UInt,
)

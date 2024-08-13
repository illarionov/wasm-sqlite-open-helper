/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.SetTimestamp
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.SetTimestampError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.EmptyPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.FileDescriptorNotOpen
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.InvalidPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.ResolvePathError.RelativePath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.asLinkOptions
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.ext.resolvePath
import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.FileTime
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.io.path.fileAttributesView

internal class NioSetTimestamp(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<SetTimestamp, SetTimestampError, Unit> {
    override fun invoke(input: SetTimestamp): Either<SetTimestampError, Unit> {
        val path: Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toSetTimestampError() }
            .getOrElse { return it.left() }
        return setTimestamp(path, input.followSymlinks, input.atimeNanoseconds, input.mtimeNanoseconds)
    }

    internal companion object {
        fun setTimestamp(
            path: Path,
            followSymlinks: Boolean,
            atimeNanoseconds: Long?,
            mtimeNanoseconds: Long?,
        ): Either<SetTimestampError, Unit> {
            val options = asLinkOptions(followSymlinks = followSymlinks)
            return Either.catch {
                path.fileAttributesView<BasicFileAttributeView>(options = options)
                    .setTimes(
                        atimeNanoseconds?.let { FileTime.from(it, NANOSECONDS) },
                        mtimeNanoseconds?.let { FileTime.from(it, NANOSECONDS) },
                        null,
                    )
            }.mapLeft {
                when (it) {
                    is IOException -> SetTimestampError.IoError("I/O error: ${it.message}")
                    else -> throw IllegalStateException("Unexpected error", it)
                }
            }
        }

        fun ResolvePathError.toSetTimestampError(): SetTimestampError = when (this) {
            is EmptyPath -> SetTimestampError.InvalidArgument(message)
            is FileDescriptorNotOpen -> SetTimestampError.BadFileDescriptor(message)
            is InvalidPath -> SetTimestampError.InvalidArgument(message)
            is NotDirectory -> SetTimestampError.NotDirectory(message)
            is RelativePath -> SetTimestampError.InvalidArgument(message)
        }
    }
}

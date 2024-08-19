/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.raise.either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.EmptyPath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.FileDescriptorNotOpen
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.InvalidPath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.NotDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.ResolvePathError.RelativePath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.asLinkOptions
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ext.resolvePath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.CheckAccess
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.CheckAccessError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.FileAccessibilityCheck.EXECUTABLE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.FileAccessibilityCheck.READABLE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.checkaccess.FileAccessibilityCheck.WRITEABLE
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable

internal class NioCheckAccess(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<CheckAccess, CheckAccessError, Unit> {
    override fun invoke(input: CheckAccess): Either<CheckAccessError, Unit> = either {
        val path = fsState.resolvePath(input.path, input.baseDirectory, input.allowEmptyPath)
            .mapLeft { it.toCheckAccessError() }
            .bind()
        if (!path.exists(options = asLinkOptions(input.followSymlinks))) {
            raise(CheckAccessError.NoEntry("File `$path` not exists"))
        }
        if (input.mode.contains(READABLE) && !path.isReadable()) {
            raise(CheckAccessError.AccessDenied("File `$path` not readable"))
        }
        if (input.mode.contains(WRITEABLE) && !path.isWritable()) {
            raise(CheckAccessError.AccessDenied("File `$path` not writable"))
        }
        if (input.mode.contains(EXECUTABLE) && !path.isExecutable()) {
            raise(CheckAccessError.AccessDenied("File `$path` not executable"))
        }
    }

    private fun ResolvePathError.toCheckAccessError(): CheckAccessError = when (this) {
        is EmptyPath -> CheckAccessError.NoEntry(message)
        is FileDescriptorNotOpen -> CheckAccessError.BadFileDescriptor(message)
        is NotDirectory -> CheckAccessError.NotDirectory(message)
        is InvalidPath -> CheckAccessError.NoEntry(message)
        is RelativePath -> CheckAccessError.NoEntry(message)
    }
}

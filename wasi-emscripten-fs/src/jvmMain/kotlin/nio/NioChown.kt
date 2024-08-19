/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ChownError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.InvalidArgument
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.IoError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NotSupported
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.PathResolver.ResolvePathError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.nio.cwd.toCommonError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.chown.Chown
import java.io.IOException
import java.nio.file.Path
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.UserPrincipalNotFoundException
import kotlin.io.path.fileAttributesView

internal class NioChown(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<Chown, ChownError, Unit> {
    override fun invoke(input: Chown): Either<ChownError, Unit> {
        val path: Path = fsState.pathResolver.resolve(input.path, input.baseDirectory, false)
            .mapLeft(ResolvePathError::toCommonError)
            .getOrElse { return it.left() }
        return setPosixUserGroup(fsState.javaFs, path, input.owner, input.group)
    }

    internal companion object {
        fun setPosixUserGroup(
            javaFs: java.nio.file.FileSystem,
            path: Path,
            owner: Int,
            group: Int,
        ): Either<ChownError, Unit> = Either.catch {
            val lookupService = javaFs.userPrincipalLookupService
            val ownerPrincipal = lookupService.lookupPrincipalByName(owner.toString())
            val groupPrincipal = lookupService.lookupPrincipalByGroupName(group.toString())
            path.fileAttributesView<PosixFileAttributeView>().run {
                setOwner(ownerPrincipal)
                setGroup(groupPrincipal)
            }
        }.mapLeft {
            when (it) {
                is UserPrincipalNotFoundException -> InvalidArgument("User not exists: ${it.message}")
                is UnsupportedOperationException -> NotSupported("Operation not supported: ${it.message}")
                is IOException -> IoError("I/O error: ${it.message}")
                else -> throw IllegalStateException("Unexpected error", it)
            }
        }
    }
}

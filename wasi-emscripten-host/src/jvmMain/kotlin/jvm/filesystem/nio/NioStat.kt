/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.Stat
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.StatError
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileModeType
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTimespec
import ru.pixnews.wasm.sqlite.open.helper.host.include.blkcnt_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.blksize_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.dev_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.gid_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.ino_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.nlink_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.off_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.uid_t
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
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.readAttributes

internal class NioStat(
    private val fsState: JvmFileSystemState,
) : NioOperationHandler<Stat, StatError, StructStat> {
    override fun invoke(input: Stat): Either<StatError, StructStat> {
        val path: Path = fsState.resolvePath(input.path, input.baseDirectory, false)
            .mapLeft { it.toStatError() }
            .getOrElse { return it.left() }
        return statCatching(path, input.followSymlinks)
    }

    internal companion object {
        const val ATTR_UNI_CTIME = "ctime"
        const val ATTR_UNI_DEV = "dev"
        const val ATTR_UNI_GID = "gid"
        const val ATTR_UNI_INO = "ino"
        const val ATTR_UNI_MODE = "mode"
        const val ATTR_UNI_NLINK = "nlink"
        const val ATTR_UNI_RDEV = "rdev"
        const val ATTR_UNI_UID = "uid"
        val UNIX_REQUESTED_ATTRIBUTES = "unix:" + listOf(
            ATTR_UNI_DEV,
            ATTR_UNI_INO,
            ATTR_UNI_MODE,
            ATTR_UNI_NLINK,
            ATTR_UNI_UID,
            ATTR_UNI_GID,
            ATTR_UNI_RDEV,
            ATTR_UNI_CTIME,
        ).joinToString(",")

        fun statCatching(
            path: Path,
            followSymlinks: Boolean,
            blockSize: ULong = 512UL,
        ): Either<StatError, StructStat> = either {
            val linkOptions = asLinkOptions(followSymlinks)

            if (!path.exists(options = linkOptions)) {
                raise(StatError.NoEntry("No such file file: `$path`"))
            }

            val basicFileAttrs: BasicFileAttributes = Either.catch {
                path.readAttributes<BasicFileAttributes>(options = linkOptions)
            }
                .mapLeft { it.readAttributesToStatError() }
                .bind()

            val unixAttrs: Map<String, Any?> = Either.catch {
                path.readAttributes(UNIX_REQUESTED_ATTRIBUTES, options = linkOptions)
            }
                .mapLeft { it.readAttributesToStatError() }
                .bind()

            val dev: dev_t = (unixAttrs[ATTR_UNI_DEV] as? Long)?.toULong() ?: 1UL
            val ino: ino_t = (unixAttrs[ATTR_UNI_INO] as? Long)?.toULong()
                ?: basicFileAttrs.fileKey().hashCode().toULong()
            val mode: FileModeType = getModeType(basicFileAttrs, unixAttrs)
            val nlink: nlink_t = (unixAttrs[ATTR_UNI_NLINK] as? Int)?.toULong() ?: 1UL
            val uid: uid_t = (unixAttrs[ATTR_UNI_UID] as? Int)?.toULong() ?: 0UL
            val gid: gid_t = (unixAttrs[ATTR_UNI_GID] as? Int)?.toULong() ?: 0UL
            val rdev: dev_t = (unixAttrs[ATTR_UNI_RDEV] as? Long)?.toULong() ?: 1UL
            val size: off_t = basicFileAttrs.size().toULong()
            val blksize: blksize_t = blockSize
            val blocks: blkcnt_t = (size + blksize - 1UL) / blksize
            val mtim: StructTimespec = basicFileAttrs.lastModifiedTime().toTimeSpec()

            val cTimeFileTime = unixAttrs[ATTR_UNI_CTIME] ?: basicFileAttrs.creationTime()
            val ctim: StructTimespec = (cTimeFileTime as? FileTime)?.toTimeSpec()
                ?: raise(StatError.IoError("Can not get file creation time"))
            val atim: StructTimespec = basicFileAttrs.lastAccessTime().toTimeSpec()

            StructStat(
                st_dev = dev,
                st_ino = ino,
                st_mode = mode,
                st_nlink = nlink,
                st_uid = uid,
                st_gid = gid,
                st_rdev = rdev,
                st_size = size,
                st_blksize = blksize,
                st_blocks = blocks,
                st_atim = atim,
                st_mtim = mtim,
                st_ctim = ctim,
            )
        }

        private fun getModeType(
            @Suppress("UnusedParameter") basicAttrs: BasicFileAttributes,
            unixAttrs: Map<String, Any?>,
        ): FileModeType {
            val unixMode = unixAttrs[ATTR_UNI_MODE] as? Int
            if (unixMode != null) {
                return FileModeType(unixMode.toUInt())
            }

            // TODO: guess from Basic mode?
            // TODO: Add type

            return FileModeType("777".toUInt(radix = 8))
        }

        private fun FileTime.toTimeSpec(): StructTimespec = toInstant().run {
            StructTimespec(
                tv_sec = epochSecond.toULong(),
                tv_nsec = nano.toULong(),
            )
        }

        private fun Throwable.readAttributesToStatError(): StatError = when (this) {
            is UnsupportedOperationException -> StatError.AccessDenied("Can not get BasicFileAttributeView")
            is IOException -> StatError.IoError("Can not read attributes: $message")
            is SecurityException -> StatError.AccessDenied("Can not read attributes: $message")
            else -> throw IllegalStateException("Unexpected error", this)
        }

        private fun ResolvePathError.toStatError(): StatError = when (this) {
            is EmptyPath -> StatError.NoEntry(message)
            is FileDescriptorNotOpen -> StatError.BadFileDescriptor(message)
            is InvalidPath -> StatError.BadFileDescriptor(message)
            is NotDirectory -> StatError.NotDirectory(message)
            is RelativePath -> StatError.BadFileDescriptor(message)
        }
    }
}

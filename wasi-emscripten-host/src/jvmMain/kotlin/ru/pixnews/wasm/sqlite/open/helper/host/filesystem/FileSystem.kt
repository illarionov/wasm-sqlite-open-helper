/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("ThrowsCount", "VARIABLE_HAS_PREFIX", "NO_BRACES_IN_CONDITIONALS_AND_LOOPS")

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import com.sun.nio.file.ExtendedOpenOption
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FileDescriptorMap
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.position
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTimespec
import ru.pixnews.wasm.sqlite.open.helper.host.include.oMaskToString
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.blkcnt_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.blksize_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.dev_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.gid_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.ino_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.mode_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.nlink_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.off_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.uid_t
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.ACCES
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.EXIST
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.INTR
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.INVAL
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.IO
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.ISDIR
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOENT
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOTDIR
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.NOTEMPTY
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno.PERM
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.channels.NonReadableChannelException
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFileAttributeView
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.io.path.exists
import kotlin.io.path.fileAttributesView
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes

public class FileSystem(
    internal val javaFs: java.nio.file.FileSystem = FileSystems.getDefault(),
    private val blockSize: ULong = 512UL,
    private val logger: Logger = Logger.getLogger(FileSystem::class.qualifiedName),
) {
    private val fileDescriptors: FileDescriptorMap = FileDescriptorMap(this)

    public fun getCwd(): String = getCwdPath().pathString

    public fun stat(
        path: String,
        followSymlinks: Boolean = true,
    ): StructStat {
        val filePath: Path = javaFs.getPath(path)
        return stat(filePath, followSymlinks)
    }

    public fun stat(
        fd: Fd,
    ): StructStat {
        val stream = fileDescriptors.get(fd) ?: throw SysException(BADF, "File descriptor `$fd` not open")
        return stat(stream.path, true)
    }

    @Suppress("CyclomaticComplexMethod", "VARIABLE_HAS_PREFIX")
    public fun stat(
        filePath: Path,
        followSymlinks: Boolean = true,
    ): StructStat {
        val linkOptions = followSymlinksToLinkOptions(followSymlinks)
        if (!filePath.exists(options = linkOptions)) {
            throw SysException(NOENT)
        }

        val basicFileAttrs = try {
            filePath.readAttributes<BasicFileAttributes>(options = linkOptions)
        } catch (e: UnsupportedOperationException) {
            throw SysException(PERM, "Can not get BasicFileAttributeView", e)
        } catch (e: IOException) {
            throw SysException(PERM, "Can not read attributes", e)
        } catch (e: SecurityException) {
            throw SysException(PERM, "Can not read attributes", e)
        }

        val unixAttrs: Map<String, Any?> = try {
            filePath.readAttributes(UNIX_REQUESTED_ATTRIBUTES, options = linkOptions)
        } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
            when (ex) {
                is UnsupportedOperationException, is IOException, is SecurityException -> emptyMap()
                else -> throw ex
            }
        }

        val dev: dev_t = (unixAttrs[ATTR_UNI_DEV] as? Long)?.toULong() ?: 1UL
        val ino: ino_t = (unixAttrs[ATTR_UNI_INO] as? Long)?.toULong()
            ?: basicFileAttrs.fileKey().hashCode().toULong()
        val mode: mode_t = getMode(basicFileAttrs, unixAttrs)
        val nlink: nlink_t = (unixAttrs[ATTR_UNI_NLINK] as? Int)?.toULong() ?: 1UL
        val uid: uid_t = (unixAttrs[ATTR_UNI_UID] as? Int)?.toULong() ?: 0UL
        val gid: gid_t = (unixAttrs[ATTR_UNI_GID] as? Int)?.toULong() ?: 0UL
        val rdev: dev_t = (unixAttrs[ATTR_UNI_RDEV] as? Long)?.toULong() ?: 1UL
        val size: off_t = basicFileAttrs.size().toULong()
        val blksize: blksize_t = blockSize
        val blocks: blkcnt_t = (size + blksize - 1UL) / blksize
        val mtim: StructTimespec = basicFileAttrs.lastModifiedTime().toTimeSpec()

        val cTimeFileTime = unixAttrs[ATTR_UNI_CTIME] ?: basicFileAttrs.creationTime()
        val ctim: StructTimespec = checkNotNull(cTimeFileTime as? FileTime) {
            "Can not get file creation time"
        }.toTimeSpec()
        val atim: StructTimespec = basicFileAttrs.lastAccessTime().toTimeSpec()

        return StructStat(
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

    public fun open(
        path: Path,
        flags: UInt,
        mode: UInt,
    ): FdChannel {
        if (path.pathString.isEmpty()) {
            throw SysException(NOENT)
        }

        val channel = try {
            val openOptions = getOpenOptions(flags)
            val fileAttributes = getOpenFileAttributes(mode)
            FileChannel.open(
                path,
                openOptions,
                fileAttributes,
            )
        } catch (iae: IllegalArgumentException) {
            throw SysException(INVAL, cause = iae)
        } catch (uoe: UnsupportedOperationException) {
            throw SysException(INVAL, cause = uoe)
        } catch (fae: FileAlreadyExistsException) {
            throw SysException(EXIST, cause = fae)
        } catch (ioe: IOException) {
            throw SysException(IO, cause = ioe)
        } catch (se: SecurityException) {
            throw SysException(PERM, cause = se)
        }

        val fd = fileDescriptors.add(path, channel)
        return fd
    }

    public fun unlinkAt(
        dirfd: Int,
        path: String,
        flags: UInt,
    ) {
        val absolutePath = resolveAbsolutePath(dirfd, path)
        @Suppress("MagicNumber")
        logger.finest { "unlinkAt($absolutePath, flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}))" }

        when (flags) {
            0U -> {
                if (absolutePath.isDirectory()) {
                    throw SysException(ISDIR)
                }
                try {
                    Files.delete(absolutePath)
                } catch (nsfe: NoSuchFileException) {
                    throw SysException(NOENT, cause = nsfe)
                } catch (ioe: IOException) {
                    throw SysException(IO, cause = ioe)
                } catch (se: SecurityException) {
                    throw SysException(ACCES, cause = se)
                }
            }

            Fcntl.AT_REMOVEDIR -> {
                if (!absolutePath.isDirectory()) {
                    throw SysException(NOTDIR)
                }
                try {
                    Files.delete(absolutePath)
                } catch (dne: DirectoryNotEmptyException) {
                    throw SysException(NOTEMPTY, cause = dne)
                } catch (ioe: IOException) {
                    throw SysException(IO, cause = ioe)
                } catch (se: SecurityException) {
                    throw SysException(ACCES, cause = se)
                }
            }

            else -> throw SysException(INVAL, "Invalid flags passed to unlinkAt()")
        }
    }

    public fun getCwdPath(): Path {
        return javaFs.getPath("").toAbsolutePath()
    }

    public fun getStreamByFd(
        fd: Fd,
    ): FdChannel {
        return fileDescriptors.get(fd) ?: throw SysException(BADF, "File descriptor $fd is not opened")
    }

    public fun seek(
        channel: FdChannel,
        offset: Long,
        whence: Whence,
    ) {
        logger.finest { "seek(${channel.fd}, $offset, $whence)" }
        val newPosition = when (whence) {
            Whence.SET -> offset
            Whence.CUR -> channel.position + offset
            Whence.END -> channel.channel.size() - offset
        }
        if (newPosition < 0) {
            throw SysException(INVAL, "Incorrect new position: $newPosition")
        }

        channel.position = newPosition
    }

    public fun read(
        channel: FdChannel,
        iovecs: Array<ByteBuffer>,
        strategy: ReadWriteStrategy = CHANGE_POSITION,
    ): ULong {
        logger.finest { "read(${channel.fd}, ${iovecs.contentToString()}, $strategy)" }

        try {
            var totalBytesRead: ULong = 0U
            when (strategy) {
                DO_NOT_CHANGE_POSITION -> {
                    var position = channel.position
                    for (iovec in iovecs) {
                        val bytesRead = channel.channel.read(iovec, position)
                        if (bytesRead > 0) {
                            position += bytesRead
                            totalBytesRead += bytesRead.toULong()
                        }
                        if (bytesRead < iovec.limit()) {
                            break
                        }
                    }
                }

                CHANGE_POSITION -> {
                    val bytesRead = channel.channel.read(iovecs)
                    totalBytesRead = if (bytesRead != -1L) bytesRead.toULong() else 0UL
                }
            }
            return totalBytesRead
        } catch (cce: ClosedChannelException) {
            throw SysException(IO, "Channel closed", cce)
        } catch (ace: AsynchronousCloseException) {
            throw SysException(IO, "Channel closed on other thread", ace)
        } catch (ci: ClosedByInterruptException) {
            throw SysException(INTR, "Interrupted", ci)
        } catch (nre: NonReadableChannelException) {
            throw SysException(BADF, "Non readable channel", nre)
        } catch (ioe: IOException) {
            throw SysException(IO, "I/o error", ioe)
        }
    }

    public fun write(
        channel: FdChannel,
        cIovecs: Array<ByteBuffer>,
        strategy: ReadWriteStrategy = CHANGE_POSITION,
    ): ULong {
        logger.finest { "write(${channel.fd}, ${cIovecs.contentToString()}, $strategy)" }
        try {
            var totalBytesWritten = 0UL
            when (strategy) {
                DO_NOT_CHANGE_POSITION -> {
                    var position = channel.position
                    for (ciovec in cIovecs) {
                        val bytesWritten = channel.channel.write(ciovec, position)
                        if (bytesWritten > 0) {
                            position += bytesWritten
                            totalBytesWritten += bytesWritten.toULong()
                        }
                        if (bytesWritten < ciovec.limit()) {
                            break
                        }
                    }
                }

                CHANGE_POSITION -> totalBytesWritten = channel.channel.write(cIovecs).toULong()
            }
            return totalBytesWritten
        } catch (cce: ClosedChannelException) {
            throw SysException(IO, "Channel closed", cce)
        } catch (ace: AsynchronousCloseException) {
            throw SysException(IO, "Channel closed on other thread", ace)
        } catch (ci: ClosedByInterruptException) {
            throw SysException(INTR, "Interrupted", ci)
        } catch (nre: NonReadableChannelException) {
            throw SysException(BADF, "Non readable channel", nre)
        } catch (ioe: IOException) {
            throw SysException(IO, "I/o error", ioe)
        }
    }

    public fun close(fd: Fd) {
        logger.finest { "close($fd)" }
        val channel = fileDescriptors.remove(fd)
        try {
            try {
                channel.channel.force(true)
            } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
                // IGNORE
                logger.log(Level.FINE, ex) { "close($fd): sync failed: ${ex.message}" }
            }
            channel.channel.close()
        } catch (ioe: IOException) {
            throw SysException(IO, "Can not close channel", ioe)
        }
    }

    public fun sync(
        fd: Fd,
        metadata: Boolean = true,
    ) {
        logger.finest { "sync($fd)" }
        val channel = getStreamByFd(fd)

        try {
            channel.channel.force(metadata)
        } catch (cce: ClosedChannelException) {
            throw SysException(IO, "Channel closed", cce)
        } catch (ioe: IOException) {
            throw SysException(IO, "I/O error", ioe)
        }
    }

    public fun chown(fd: Fd, owner: Int, group: Int) {
        logger.finest { "chown($fd, $owner, $group)" }
        val channel = getStreamByFd(fd)
        try {
            val lookupService = javaFs.userPrincipalLookupService
            val ownerPrincipal = lookupService.lookupPrincipalByName(owner.toString())
            val groupPrincipal = lookupService.lookupPrincipalByGroupName(group.toString())
            channel.path.fileAttributesView<PosixFileAttributeView>().run {
                setOwner(ownerPrincipal)
                setGroup(groupPrincipal)
            }
        } catch (uoe: UnsupportedOperationException) {
            throw SysException(ACCES, cause = uoe)
        } catch (ioe: IOException) {
            throw SysException(IO, "I/O error", ioe)
        }
    }

    public fun ftruncate(fd: Fd, length: ULong) {
        logger.finest { "ftruncate($fd, $length)" }
        val channel = getStreamByFd(fd)
        try {
            channel.channel.truncate(length.toLong())
            // TODO: extend file size to length?
        } catch (nve: NonReadableChannelException) {
            throw SysException(INVAL, "Read-only channel", nve)
        } catch (cce: ClosedChannelException) {
            throw SysException(BADF, "Channel closed", cce)
        } catch (iae: IllegalArgumentException) {
            throw SysException(INVAL, "Negative length", iae)
        } catch (ioe: IOException) {
            throw SysException(IO, ioe.message, ioe)
        }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun getOpenOptions(
        flags: UInt,
    ): Set<OpenOption> {
        val options: MutableSet<OpenOption> = mutableSetOf()
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
            logger.info { """O_NONBLOCK not implemented""" }
        }

        if (flags and Fcntl.O_ASYNC != 0U) {
            logger.info { """O_ASYNC not implemented""" }
        }

        if (flags and (Fcntl.O_DSYNC or Fcntl.O_SYNC) != 0U) {
            options += StandardOpenOption.SYNC
        }

        if (flags and Fcntl.O_DIRECT != 0U) {
            options += ExtendedOpenOption.DIRECT
        }

        if (flags and Fcntl.O_DIRECTORY != 0U) {
            throw SysException(ISDIR, "O_DIRECTORY not implemented")
        }

        if (flags and Fcntl.O_NOFOLLOW != 0U) {
            options += LinkOption.NOFOLLOW_LINKS
        }
        if (flags and Fcntl.O_NOATIME != 0U) {
            logger.info { "O_NOATIME not implemented" }
        }
        if (flags and Fcntl.O_CLOEXEC != 0U) {
            logger.finest { "O_CLOEXEC not implemented" }
        }

        if (flags and Fcntl.O_PATH != 0U) {
            throw SysException(ISDIR, """O_PATH not implemented""")
        }

        if (flags and Fcntl.O_TMPFILE != 0U) {
            logger.info { "O_TMPFILE not implemented" }
            options += StandardOpenOption.DELETE_ON_CLOSE
        }

        return options
    }

    private fun getOpenFileAttributes(
        mode: UInt,
    ): FileAttribute<Set<PosixFilePermission>> {
        val permissions: MutableSet<PosixFilePermission> = mutableSetOf()

        if (mode and Fcntl.S_IRUSR != 0U) permissions += PosixFilePermission.OWNER_READ
        if (mode and Fcntl.S_IWUSR != 0U) permissions += PosixFilePermission.OWNER_WRITE
        if (mode and Fcntl.S_IXUSR != 0U) permissions += PosixFilePermission.OWNER_EXECUTE

        if (mode and Fcntl.S_IRGRP != 0U) permissions += PosixFilePermission.GROUP_READ
        if (mode and Fcntl.S_IWGRP != 0U) permissions += PosixFilePermission.GROUP_WRITE
        if (mode and Fcntl.S_IXGRP != 0U) permissions += PosixFilePermission.GROUP_EXECUTE

        if (mode and Fcntl.S_IROTH != 0U) permissions += PosixFilePermission.OTHERS_READ
        if (mode and Fcntl.S_IWOTH != 0U) permissions += PosixFilePermission.OTHERS_WRITE
        if (mode and Fcntl.S_IXOTH != 0U) permissions += PosixFilePermission.OTHERS_EXECUTE

        mode.and(SUPPORTED_MODES.inv()).let { unsupportedModes ->
            if (unsupportedModes != 0U) {
                @Suppress("MagicNumber")
                logger.info { "Mode 0${unsupportedModes.toString(8)} not supported" }
            }
        }

        return PosixFilePermissions.asFileAttribute(permissions)
    }

    private companion object {
        private const val ATTR_UNI_CTIME = "ctime"
        private const val ATTR_UNI_DEV = "dev"
        private const val ATTR_UNI_GID = "gid"
        private const val ATTR_UNI_INO = "ino"
        private const val ATTR_UNI_MODE = "mode"
        private const val ATTR_UNI_NLINK = "nlink"
        private const val ATTR_UNI_RDEV = "rdev"
        private const val ATTR_UNI_UID = "uid"
        private val SUPPORTED_MODES = Fcntl.S_IRUSR or Fcntl.S_IWUSR or Fcntl.S_IXUSR or
                Fcntl.S_IRGRP or Fcntl.S_IWGRP or Fcntl.S_IXGRP or
                Fcntl.S_IROTH or Fcntl.S_IWOTH or Fcntl.S_IXOTH
        private val UNIX_REQUESTED_ATTRIBUTES = "unix:" + listOf(
            ATTR_UNI_DEV,
            ATTR_UNI_INO,
            ATTR_UNI_MODE,
            ATTR_UNI_NLINK,
            ATTR_UNI_UID,
            ATTR_UNI_GID,
            ATTR_UNI_RDEV,
            ATTR_UNI_CTIME,
        ).joinToString(",")

        private fun getMode(
            @Suppress("UnusedParameter") basicAttrs: BasicFileAttributes,
            unixAttrs: Map<String, Any?>,
        ): mode_t {
            val unixMode = unixAttrs[ATTR_UNI_MODE] as? Int
            if (unixMode != null) {
                return unixMode.toULong()
            }

            // TODO: guess from Basic mode?

            return "777".toULong(radix = 8)
        }

        private fun FileTime.toTimeSpec(): StructTimespec = toInstant().run {
            StructTimespec(
                tv_sec = epochSecond.toULong(),
                tv_nsec = nano.toULong(),
            )
        }

        private fun followSymlinksToLinkOptions(
            followSymlinks: Boolean,
        ): Array<LinkOption> = if (followSymlinks) {
            arrayOf()
        } else {
            arrayOf(LinkOption.NOFOLLOW_LINKS)
        }
    }
}

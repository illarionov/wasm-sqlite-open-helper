/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("ThrowsCount", "VARIABLE_HAS_PREFIX", "NO_BRACES_IN_CONDITIONALS_AND_LOOPS")

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import com.sun.nio.file.ExtendedOpenOption
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FileDescriptorMap
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.position
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.resolveAbsolutePath
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.resolvePosition
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTimespec
import ru.pixnews.wasm.sqlite.open.helper.host.include.blkcnt_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.blksize_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.dev_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.gid_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.ino_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.nlink_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.oMaskToString
import ru.pixnews.wasm.sqlite.open.helper.host.include.off_t
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.include.uid_t
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
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFileAttributeView
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.io.path.exists
import kotlin.io.path.fileAttributesView
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.time.Duration

public class FileSystem(
    rootLogger: Logger,
    internal val javaFs: java.nio.file.FileSystem = FileSystems.getDefault(),
    private val blockSize: ULong = 512UL,
) {
    private val logger: Logger = rootLogger.withTag(FileSystem::class.qualifiedName!!)
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
        val mode: FileMode = getMode(basicFileAttrs, unixAttrs)
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
        mode: FileMode,
    ): FdChannel {
        if (path.pathString.isEmpty()) {
            throw SysException(NOENT)
        }

        val channel = try {
            val openOptions = getOpenOptions(flags)
            val fileAttributes = mode.toPosixFilePermissions()
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
        dirfd: DirFd,
        path: String,
        flags: UInt,
    ) {
        val absolutePath = resolveAbsolutePath(dirfd, path)
        @Suppress("MagicNumber")
        logger.v { "unlinkAt($absolutePath, flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}))" }

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
        logger.v { "seek(${channel.fd}, $offset, $whence)" }
        val newPosition = channel.resolvePosition(offset, whence)
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
        logger.v { "read(${channel.fd}, ${iovecs.contentToString()}, $strategy)" }

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
        logger.v { "write(${channel.fd}, ${cIovecs.contentToString()}, $strategy)" }
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
        logger.v { "close($fd)" }
        val channel = fileDescriptors.remove(fd)
        try {
            try {
                channel.channel.force(true)
            } catch (@Suppress("TooGenericExceptionCaught") ex: Throwable) {
                // IGNORE
                logger.v(ex) { "close($fd): sync failed: ${ex.message}" }
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
        logger.v { "sync($fd)" }
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
        logger.v { "chown($fd, $owner, $group)" }
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
        logger.v { "ftruncate($fd, $length)" }
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

    public fun mkdirAt(dirFd: DirFd, path: String, mode: FileMode) {
        val absolutePath = resolveAbsolutePath(dirFd, path)
        logger.v { "mkdirAt($absolutePath, $mode}" }
        try {
            Files.createDirectory(absolutePath, mode.toPosixFilePermissions())
        } catch (uoe: UnsupportedOperationException) {
            throw SysException(PERM, "Unsupported file mode", uoe)
        } catch (fae: FileAlreadyExistsException) {
            throw SysException(EXIST, "`$absolutePath` exists", fae)
        } catch (ioe: IOException) {
            throw SysException(IO, ioe.message, ioe)
        }
    }

    public fun setTimesAt(
        dirFd: DirFd,
        path: String,
        atime: Duration?,
        mtime: Duration?,
        noFolowSymlinks: Boolean,
    ) {
        val absolutePath = resolveAbsolutePath(dirFd, path)
        logger.v { "utimensat($absolutePath, $atime, $mtime, $noFolowSymlinks)" }
        try {
            val options = if (noFolowSymlinks) {
                arrayOf(NOFOLLOW_LINKS)
            } else {
                arrayOf()
            }
            absolutePath.fileAttributesView<BasicFileAttributeView>(options = options)
                .setTimes(
                    mtime?.let { FileTime.from(it.inWholeNanoseconds, NANOSECONDS) },
                    atime?.let { FileTime.from(it.inWholeNanoseconds, NANOSECONDS) },
                    null,
                )
        } catch (ioe: IOException) {
            logger.v(ioe) { "utimensat($absolutePath, $atime, $mtime, $noFolowSymlinks) error: $ioe" }
            throw SysException(IO, ioe.message, ioe)
        }
    }

    public fun rmdir(path: String) {
        logger.v { "rmdir($path}" }
        try {
            val absolutePath = javaFs.getPath(path)
            if (!absolutePath.isDirectory()) {
                throw SysException(NOTDIR, "`$path` is not a directory")
            }
            if (absolutePath.endsWith(".")) {
                throw SysException(INVAL, "`$path` has . as last component")
            }
            Files.delete(absolutePath)
        } catch (nse: NoSuchFileException) {
            throw SysException(NOENT, "`$path` not exists", nse)
        } catch (dne: DirectoryNotEmptyException) {
            throw SysException(NOTEMPTY, "`$path` not empty", dne)
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
            logger.i { """O_NONBLOCK not implemented""" }
        }

        if (flags and Fcntl.O_ASYNC != 0U) {
            logger.i { """O_ASYNC not implemented""" }
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
            logger.i { "O_NOATIME not implemented" }
        }
        if (flags and Fcntl.O_CLOEXEC != 0U) {
            logger.v { "O_CLOEXEC not implemented" }
        }

        if (flags and Fcntl.O_PATH != 0U) {
            throw SysException(ISDIR, """O_PATH not implemented""")
        }

        if (flags and Fcntl.O_TMPFILE != 0U) {
            logger.i { "O_TMPFILE not implemented" }
            options += StandardOpenOption.DELETE_ON_CLOSE
        }

        return options
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
        ): FileMode {
            val unixMode = unixAttrs[ATTR_UNI_MODE] as? Int
            if (unixMode != null) {
                return FileMode(unixMode.toUInt())
            }

            // TODO: guess from Basic mode?

            return FileMode("777".toUInt(radix = 8))
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

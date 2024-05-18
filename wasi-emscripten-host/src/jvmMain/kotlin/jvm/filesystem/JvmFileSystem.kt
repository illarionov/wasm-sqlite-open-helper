/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("ThrowsCount", "NO_BRACES_IN_CONDITIONALS_AND_LOOPS")

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem

import com.sun.nio.file.ExtendedOpenOption
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.common.api.clear
import ru.pixnews.wasm.sqlite.open.helper.common.api.contains
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.DO_NOT_CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd.Cwd
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd.FileDescriptor
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileAccessibilityCheck
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
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
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.JvmPath.Companion.toCommonPath
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FdFileChannel
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.FileDescriptorMap
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.addAdvisorylock
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.position
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.removeAdvisoryLock
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.fd.resolveWhencePosition
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
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.PosixFileAttributeView
import java.util.concurrent.TimeUnit.NANOSECONDS
import kotlin.io.path.exists
import kotlin.io.path.fileAttributesView
import kotlin.io.path.isDirectory
import kotlin.io.path.isExecutable
import kotlin.io.path.isReadable
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.pathString
import kotlin.io.path.readAttributes
import kotlin.io.path.setPosixFilePermissions
import kotlin.time.Duration

public class JvmFileSystem(
    rootLogger: Logger,
    internal val javaFs: java.nio.file.FileSystem = FileSystems.getDefault(),
    private val blockSize: ULong = 512UL,
) : FileSystem<JvmPath> {
    private val logger: Logger = rootLogger.withTag("FileSystem")
    private val fileDescriptors: FileDescriptorMap = FileDescriptorMap(this)

    override fun getCwd(): String = getCwdPath().nio.pathString

    override fun stat(
        path: String,
        followSymlinks: Boolean,
    ): StructStat {
        val filePath = javaFs.getPath(path).toCommonPath()
        return stat(filePath, followSymlinks)
    }

    override fun stat(
        fd: Fd,
    ): StructStat {
        val stream = fileDescriptors.get(fd) ?: throw SysException(BADF, "File descriptor `$fd` not open")
        return stat(stream.path, true)
    }

    @Suppress("CyclomaticComplexMethod")
    override fun stat(
        filePath: JvmPath,
        followSymlinks: Boolean,
    ): StructStat {
        val linkOptions = followSymlinksToLinkOptions(followSymlinks)
        if (!filePath.nio.exists(options = linkOptions)) {
            throw SysException(NOENT)
        }

        val basicFileAttrs = try {
            filePath.nio.readAttributes<BasicFileAttributes>(options = linkOptions)
        } catch (e: UnsupportedOperationException) {
            throw SysException(PERM, "Can not get BasicFileAttributeView", e)
        } catch (e: IOException) {
            throw SysException(PERM, "Can not read attributes", e)
        } catch (e: SecurityException) {
            throw SysException(PERM, "Can not read attributes", e)
        }

        val unixAttrs: Map<String, Any?> = try {
            filePath.nio.readAttributes(UNIX_REQUESTED_ATTRIBUTES, options = linkOptions)
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

    override fun resolveAbsolutePath(dirFd: DirFd, path: String, allowEmpty: Boolean): JvmPath {
        val nioPath = javaFs.getPath(path)

        if (nioPath.isAbsolute) {
            return nioPath.toCommonPath()
        }

        val root: JvmPath = when (dirFd) {
            is Cwd -> getCwdPath()
            is FileDescriptor -> try {
                getPath(dirFd.fd)
            } catch (e: SysException) {
                throw SysException(BADF, "File descriptor $dirFd is not open", cause = e)
            }
        }

        if (nioPath.pathString.isEmpty() && !allowEmpty) {
            throw SysException(NOENT)
        }
        return root.nio.resolve(nioPath).toCommonPath()
    }

    override fun getPath(fd: Fd): JvmPath {
        return getStreamByFd(fd).path
    }

    override fun isRegularFile(fd: Fd): Boolean {
        return getStreamByFd(fd).path.nio.isRegularFile()
    }

    override fun open(
        path: JvmPath,
        flags: UInt,
        mode: FileMode,
    ): Fd {
        if (path.nio.pathString.isEmpty()) {
            throw SysException(NOENT)
        }

        val nioChannel = try {
            val openOptions = getOpenOptions(flags)
            val fileAttributes = mode.toPosixFilePermissions().asFileAttribute()
            FileChannel.open(
                path.nio,
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

        val jvmChannel: FdFileChannel = fileDescriptors.add(path, nioChannel)
        return jvmChannel.fd
    }

    override fun unlinkAt(
        dirfd: DirFd,
        path: String,
        flags: UInt,
    ) {
        val absolutePath = resolveAbsolutePath(dirfd, path)
        logger.v { "unlinkAt($absolutePath, flags: 0${flags.toString(8)} (${Fcntl.oMaskToString(flags)}))" }

        when (flags) {
            0U -> {
                if (absolutePath.nio.isDirectory()) {
                    throw SysException(ISDIR)
                }
                try {
                    Files.delete(absolutePath.nio)
                } catch (nsfe: NoSuchFileException) {
                    throw SysException(NOENT, cause = nsfe)
                } catch (ioe: IOException) {
                    throw SysException(IO, cause = ioe)
                } catch (se: SecurityException) {
                    throw SysException(ACCES, cause = se)
                }
            }

            Fcntl.AT_REMOVEDIR -> {
                if (!absolutePath.nio.isDirectory()) {
                    throw SysException(NOTDIR)
                }
                try {
                    Files.delete(absolutePath.nio)
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

    override fun getCwdPath(): JvmPath {
        return javaFs.getPath("").toAbsolutePath().toCommonPath()
    }

    override fun resolveWhencePosition(fd: Fd, offset: Long, whence: Whence): Long {
        return getStreamByFd(fd).resolveWhencePosition(offset, whence)
    }

    internal fun getStreamByFd(fd: Fd): FdFileChannel {
        return fileDescriptors.get(fd) ?: throw SysException(BADF, "File descriptor $fd is not opened")
    }

    public fun getNioFileChannelByFd(fd: Fd): FileChannel {
        return getStreamByFd(fd).channel
    }

    override fun seek(
        fd: Fd,
        offset: Long,
        whence: Whence,
    ): Long {
        val channel = getStreamByFd(fd)
        logger.v { "seek($fd, $offset, $whence)" }
        val newPosition = channel.resolveWhencePosition(offset, whence)
        if (newPosition < 0) {
            throw SysException(INVAL, "Incorrect new position: $newPosition")
        }

        channel.position = newPosition
        return newPosition
    }

    override fun read(
        fd: Fd,
        iovecs: List<FileSystemByteBuffer>,
        strategy: ReadWriteStrategy,
    ): ULong {
        logger.v { "read($fd, $iovecs, $strategy)" }
        val channel = getStreamByFd(fd)

        try {
            var totalBytesRead: ULong = 0U
            when (strategy) {
                DO_NOT_CHANGE_POSITION -> {
                    var position = channel.position
                    for (iovec in iovecs) {
                        val byteBuffer = iovec.asByteBuffer()
                        val bytesRead = channel.channel.read(byteBuffer, position)
                        if (bytesRead > 0) {
                            position += bytesRead
                            totalBytesRead += bytesRead.toULong()
                        }
                        if (bytesRead < byteBuffer.limit()) {
                            break
                        }
                    }
                }

                CHANGE_POSITION -> {
                    val byteBuffers = Array(iovecs.size) { iovecs[it].asByteBuffer() }
                    val bytesRead = channel.channel.read(byteBuffers)
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

    override fun write(
        fd: Fd,
        cIovecs: List<FileSystemByteBuffer>,
        strategy: ReadWriteStrategy,
    ): ULong {
        logger.v { "write($fd, $cIovecs, $strategy)" }
        val channel = getStreamByFd(fd)
        try {
            var totalBytesWritten = 0UL
            when (strategy) {
                DO_NOT_CHANGE_POSITION -> {
                    var position = channel.position
                    for (ciovec in cIovecs) {
                        val byteBuffer = ciovec.asByteBuffer()
                        val bytesWritten = channel.channel.write(byteBuffer, position)
                        if (bytesWritten > 0) {
                            position += bytesWritten
                            totalBytesWritten += bytesWritten.toULong()
                        }
                        if (bytesWritten < byteBuffer.limit()) {
                            break
                        }
                    }
                }

                CHANGE_POSITION -> {
                    val byteBuffers = Array(cIovecs.size) { cIovecs[it].asByteBuffer() }
                    totalBytesWritten = channel.channel.write(byteBuffers).toULong()
                }
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

    override fun close(fd: Fd) {
        logger.v { "close($fd)" }
        val channel = fileDescriptors.remove(fd)
        try {
            channel.channel.close()
        } catch (ioe: IOException) {
            throw SysException(IO, "Can not close channel", ioe)
        }
    }

    override fun sync(
        fd: Fd,
        metadata: Boolean,
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

    override fun chown(fd: Fd, owner: Int, group: Int) {
        logger.v { "chown($fd, $owner, $group)" }
        val channel = getStreamByFd(fd)
        try {
            val lookupService = javaFs.userPrincipalLookupService
            val ownerPrincipal = lookupService.lookupPrincipalByName(owner.toString())
            val groupPrincipal = lookupService.lookupPrincipalByGroupName(group.toString())
            channel.path.nio.fileAttributesView<PosixFileAttributeView>().run {
                setOwner(ownerPrincipal)
                setGroup(groupPrincipal)
            }
        } catch (uoe: UnsupportedOperationException) {
            throw SysException(ACCES, cause = uoe)
        } catch (ioe: IOException) {
            throw SysException(IO, "I/O error", ioe)
        }
    }

    override fun chmod(fd: Fd, mode: FileMode) {
        logger.v { "chmod($fd, $mode)" }
        val channel = getStreamByFd(fd)
        chmod(channel.path, mode)
    }

    override fun chmod(path: String, mode: FileMode) {
        val javaPath = javaFs.getPath(path)
        chmod(javaPath.toCommonPath(), mode)
    }

    override fun chmod(
        javaPath: JvmPath,
        mode: FileMode,
    ) {
        try {
            javaPath.nio.setPosixFilePermissions(mode.toPosixFilePermissions())
        } catch (uoe: UnsupportedOperationException) {
            throw SysException(PERM, "Read-only channel", uoe)
        } catch (cce: ClassCastException) {
            throw SysException(INVAL, "Invalid flags", cce)
        } catch (ioe: IOException) {
            throw SysException(IO, ioe.message, ioe)
        } catch (sse: SecurityException) {
            throw SysException(ACCES, "Security Exception", sse)
        }
    }

    override fun ftruncate(fd: Fd, length: ULong) {
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

    override fun mkdirAt(dirFd: DirFd, path: String, mode: FileMode) {
        val absolutePath = resolveAbsolutePath(dirFd, path)
        logger.v { "mkdirAt($absolutePath, $mode}" }
        try {
            Files.createDirectory(absolutePath.nio, mode.toPosixFilePermissions().asFileAttribute())
        } catch (uoe: UnsupportedOperationException) {
            throw SysException(PERM, "Unsupported file mode", uoe)
        } catch (fae: FileAlreadyExistsException) {
            throw SysException(EXIST, "`$absolutePath` exists", fae)
        } catch (ioe: IOException) {
            throw SysException(IO, ioe.message, ioe)
        }
    }

    override fun setTimesAt(
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
            absolutePath.nio.fileAttributesView<BasicFileAttributeView>(options = options)
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

    override fun rmdir(path: String) {
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

    override fun faccessat(
        dirFd: DirFd,
        path: String,
        mode: FileAccessibilityCheck,
        flags: UInt,
    ) {
        val absolutePath = resolveAbsolutePath(dirFd, path)

        if (mode.clear(FileAccessibilityCheck.MASK).mask != 0U) {
            throw SysException(INVAL, "Unrecognized check mode $mode")
        }
        val options = if ((flags and Fcntl.AT_SYMLINK_NOFOLLOW) != 0U) {
            arrayOf(NOFOLLOW_LINKS)
        } else {
            arrayOf()
        }
        if (!absolutePath.nio.exists(options = options)) {
            throw SysException(NOENT, "File `$absolutePath` not exists")
        }
        if (mode.contains(FileAccessibilityCheck.R_OK) && !absolutePath.nio.isReadable()) {
            throw SysException(ACCES, "File `$absolutePath` not readable")
        }
        if (mode.contains(FileAccessibilityCheck.W_OK) && !absolutePath.nio.isWritable()) {
            throw SysException(ACCES, "File `$absolutePath` not writable")
        }
        if (mode.contains(FileAccessibilityCheck.X_OK) && !absolutePath.nio.isExecutable()) {
            throw SysException(ACCES, "File `$absolutePath` not executable")
        }
    }

    override fun addAdvisoryLock(fd: Fd, flock: StructFlock) {
        val channel = getStreamByFd(fd)
        addAdvisorylock(channel, flock)
    }

    override fun removeAdvisoryLock(fd: Fd, flock: StructFlock) {
        val channel = getStreamByFd(fd)
        removeAdvisoryLock(channel, flock)
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
            arrayOf(NOFOLLOW_LINKS)
        }
    }
}

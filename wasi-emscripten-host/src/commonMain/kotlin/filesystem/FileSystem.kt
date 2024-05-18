/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileAccessibilityCheck
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence
import kotlin.time.Duration

public interface FileSystem<P : Path> {
    public fun getCwd(): String
    public fun stat(
        path: String,
        followSymlinks: Boolean = true,
    ): StructStat

    public fun resolveAbsolutePath(
        dirFd: DirFd,
        path: String,
        allowEmpty: Boolean = false,
    ): P

    public fun stat(
        fd: Fd,
    ): StructStat

    public fun stat(
        filePath: P,
        followSymlinks: Boolean = true,
    ): StructStat

    public fun open(
        path: P,
        flags: UInt,
        mode: FileMode,
    ): Fd

    public fun getPath(
        fd: Fd,
    ): P

    public fun isRegularFile(
        fd: Fd,
    ): Boolean

    public fun unlinkAt(
        dirfd: DirFd,
        path: String,
        flags: UInt,
    )

    public fun getCwdPath(): P

    public fun resolveWhencePosition(
        fd: Fd,
        offset: Long,
        whence: Whence,
    ): Long

    public fun seek(
        fd: Fd,
        offset: Long,
        whence: Whence,
    ): Long

    public fun read(
        fd: Fd,
        iovecs: List<FileSystemByteBuffer>,
        strategy: ReadWriteStrategy = ReadWriteStrategy.CHANGE_POSITION,
    ): ULong

    public fun write(
        fd: Fd,
        cIovecs: List<FileSystemByteBuffer>,
        strategy: ReadWriteStrategy = ReadWriteStrategy.CHANGE_POSITION,
    ): ULong

    public fun close(fd: Fd)
    public fun sync(
        fd: Fd,
        metadata: Boolean = true,
    )

    public fun chown(fd: Fd, owner: Int, group: Int)
    public fun chmod(fd: Fd, mode: FileMode)
    public fun chmod(path: String, mode: FileMode)
    public fun chmod(
        javaPath: P,
        mode: FileMode,
    )

    public fun ftruncate(fd: Fd, length: ULong)
    public fun mkdirAt(dirFd: DirFd, path: String, mode: FileMode)
    public fun setTimesAt(
        dirFd: DirFd,
        path: String,
        atime: Duration?,
        mtime: Duration?,
        noFolowSymlinks: Boolean,
    )

    public fun rmdir(path: String)

    public fun faccessat(
        dirFd: DirFd,
        path: String,
        mode: FileAccessibilityCheck,
        flags: UInt,
    )

    public fun addAdvisoryLock(
        fd: Fd,
        flock: StructFlock,
    )

    public fun removeAdvisoryLock(
        fd: Fd,
        flock: StructFlock,
    )
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.stub

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileAccessibilityCheck
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence

@InternalWasmSqliteHelperApi
public class NotImplementedFileSystem(
    rootLogger: Logger,
) : FileSystem<NotImplementedPath> {
    @Suppress("UnusedPrivateProperty") private val logger = rootLogger.withTag("PosixFileSystem")
    override fun stat(path: String, followSymlinks: Boolean): StructStat {
        TODO("Not yet implemented")
    }

    override fun stat(fd: Fd): StructStat {
        TODO("Not yet implemented")
    }

    override fun resolveAbsolutePath(dirFd: DirFd, path: String, allowEmpty: Boolean): NotImplementedPath {
        TODO("Not yet implemented")
    }

    override fun getPath(fd: Fd): NotImplementedPath {
        TODO("Not yet implemented")
    }

    override fun getCwdPath(): NotImplementedPath {
        TODO("Not yet implemented")
    }

    override fun isRegularFile(fd: Fd): Boolean {
        TODO("Not yet implemented")
    }

    override fun unlinkAt(dirfd: DirFd, path: String, flags: UInt) {
        TODO("Not yet implemented")
    }

    override fun resolveWhencePosition(fd: Fd, offset: Long, whence: Whence): Long {
        TODO("Not yet implemented")
    }

    override fun seek(fd: Fd, offset: Long, whence: Whence): Long {
        TODO("Not yet implemented")
    }

    override fun read(fd: Fd, iovecs: List<FileSystemByteBuffer>, strategy: ReadWriteStrategy): ULong {
        TODO("Not yet implemented")
    }

    override fun write(fd: Fd, cIovecs: List<FileSystemByteBuffer>, strategy: ReadWriteStrategy): ULong {
        TODO("Not yet implemented")
    }

    override fun close(fd: Fd) {
        TODO("Not yet implemented")
    }

    override fun sync(fd: Fd, metadata: Boolean) {
        TODO("Not yet implemented")
    }

    override fun chown(fd: Fd, owner: Int, group: Int) {
        TODO("Not yet implemented")
    }

    override fun chmod(fd: Fd, mode: FileMode) {
        TODO("Not yet implemented")
    }

    override fun chmod(path: String, mode: FileMode) {
        TODO("Not yet implemented")
    }

    override fun ftruncate(fd: Fd, length: ULong) {
        TODO("Not yet implemented")
    }

    override fun mkdirAt(dirFd: DirFd, path: String, mode: FileMode) {
        TODO("Not yet implemented")
    }

    override fun setTimesAt(
        dirFd: DirFd,
        path: String,
        atimeNanoseconds: Long?,
        mtimeNanoseconds: Long?,
        noFolowSymlinks: Boolean,
    ) {
        TODO("Not yet implemented")
    }

    override fun rmdir(path: String) {
        TODO("Not yet implemented")
    }

    override fun faccessat(dirFd: DirFd, path: String, mode: FileAccessibilityCheck, flags: UInt) {
        TODO("Not yet implemented")
    }

    override fun readLinkAt(dirFd: DirFd, path: String): String {
        TODO("Not yet implemented")
    }

    override fun addAdvisoryLock(fd: Fd, flock: StructFlock) {
        TODO("Not yet implemented")
    }

    override fun removeAdvisoryLock(fd: Fd, flock: StructFlock) {
        TODO("Not yet implemented")
    }

    override fun chmod(javaPath: NotImplementedPath, mode: FileMode) {
        TODO("Not yet implemented")
    }

    override fun open(path: NotImplementedPath, flags: UInt, mode: FileMode): Fd {
        TODO("Not yet implemented")
    }

    override fun stat(filePath: NotImplementedPath, followSymlinks: Boolean): StructStat {
        TODO("Not yet implemented")
    }
}

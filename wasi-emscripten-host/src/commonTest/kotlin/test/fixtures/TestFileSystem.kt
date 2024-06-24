/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemByteBuffer
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.Path
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.include.DirFd
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileAccessibilityCheck
import ru.pixnews.wasm.sqlite.open.helper.host.include.FileMode
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructFlock
import ru.pixnews.wasm.sqlite.open.helper.host.include.sys.StructStat
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Whence

open class TestFileSystem : FileSystem<Path> {
    var readLinkAtHandler: (dirFd: DirFd, path: String) -> String = { _, _ ->
        error("Not implemented")
    }

    override fun stat(path: String, followSymlinks: Boolean): StructStat {
        error("Not implemented")
    }

    override fun stat(fd: Fd): StructStat {
        error("Not implemented")
    }

    override fun resolveAbsolutePath(dirFd: DirFd, path: String, allowEmpty: Boolean): Path {
        error("Not implemented")
    }

    override fun getPath(fd: Fd): Path {
        error("Not implemented")
    }

    override fun isRegularFile(fd: Fd): Boolean {
        error("Not implemented")
    }

    override fun unlinkAt(dirfd: DirFd, path: String, flags: UInt) {
        error("Not implemented")
    }

    override fun getCwdPath(): Path {
        error("Not implemented")
    }

    override fun resolveWhencePosition(fd: Fd, offset: Long, whence: Whence): Long {
        error("Not implemented")
    }

    override fun seek(fd: Fd, offset: Long, whence: Whence): Long {
        error("Not implemented")
    }

    override fun read(fd: Fd, iovecs: List<FileSystemByteBuffer>, strategy: ReadWriteStrategy): ULong {
        error("Not implemented")
    }

    override fun write(fd: Fd, cIovecs: List<FileSystemByteBuffer>, strategy: ReadWriteStrategy): ULong {
        error("Not implemented")
    }

    override fun close(fd: Fd) {
        error("Not implemented")
    }

    override fun sync(fd: Fd, metadata: Boolean) {
        error("Not implemented")
    }

    override fun chown(fd: Fd, owner: Int, group: Int) {
        error("Not implemented")
    }

    override fun chmod(fd: Fd, mode: FileMode) {
        error("Not implemented")
    }

    override fun chmod(path: String, mode: FileMode) {
        error("Not implemented")
    }

    override fun ftruncate(fd: Fd, length: ULong) {
        error("Not implemented")
    }

    override fun mkdirAt(dirFd: DirFd, path: String, mode: FileMode) {
        error("Not implemented")
    }

    override fun setTimesAt(
        dirFd: DirFd,
        path: String,
        atimeNanoseconds: Long?,
        mtimeNanoseconds: Long?,
        noFolowSymlinks: Boolean,
    ) {
        error("Not yet implemented")
    }

    override fun rmdir(path: String) {
        error("Not implemented")
    }

    override fun faccessat(dirFd: DirFd, path: String, mode: FileAccessibilityCheck, flags: UInt) {
        error("Not implemented")
    }

    override fun readLinkAt(dirFd: DirFd, path: String): String {
        return readLinkAtHandler(dirFd, path)
    }

    override fun addAdvisoryLock(fd: Fd, flock: StructFlock) {
        error("Not implemented")
    }

    override fun removeAdvisoryLock(fd: Fd, flock: StructFlock) {
        error("Not implemented")
    }

    override fun chmod(javaPath: Path, mode: FileMode) {
        error("Not implemented")
    }

    override fun open(path: Path, flags: UInt, mode: FileMode): Fd {
        error("Not implemented")
    }

    override fun stat(filePath: Path, followSymlinks: Boolean): StructStat {
        error("Not implemented")
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op

import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd

internal object Messages {
    internal const val AGAIN = "Operation would block"
    internal const val ACCESS = "Permission denied"
    internal const val BADF = "Bad file descriptor"
    internal const val BUSY = "File busy"
    internal const val DQUOT = "Disk quota exhausted"
    internal const val FBIG = "File too large"
    internal const val INTR = "Interrupted"
    internal const val INVAL = "Invalid argument"
    internal const val IO = "I/O error"
    internal const val ISDIR = "The named file is a directory"
    internal const val LOOP = "Too many levels of symbolic links"
    internal const val MFILE = "Per-process limit for open file descriptors has been reached"
    internal const val MLINK = "Too many links"
    internal const val NAMETOOLONG = "Pathname too long"
    internal const val NFILE = "System-wide limit for open file descriptors has been reached"
    internal const val NOBUFS = "No buffer space available"
    internal const val NOENT = "No such file or directory"
    internal const val NOLCK = "System limit on locks exceeded"
    internal const val NOSPC = "No space left on device"
    internal const val NOTCAPABLE = "Capabilities insufficient"
    internal const val NOTDIR = "Path is not a directory"
    internal const val NOTEMPTY = "Directory not empty"
    internal const val NOTSUP = "Not supported"
    internal const val NOTTY = "File descriptor is not valid"
    internal const val NXIO = "No such device or address"
    internal const val OVERFLOW = "Resulting file offset too large"
    internal const val PERM = "Permission denied"
    internal const val PIPE = "Broken pipe"
    internal const val ROFS = "The named file resides on a read-only file system"
    internal const val TXTBSY = "File busy"

    internal fun fileDescriptorNotOpenedMessage(fd: Fd) = "File descriptor `$fd` is not opened"
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.filesystem.fd

import ru.pixnews.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno.BADF
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno.INVAL
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Errno.IO
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.Fd
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.file.Path

public class FdChannel(
    public val fileSystem: FileSystem,
    public val fd: Fd,
    public val path: Path,
    public val channel: FileChannel,
)

public var FdChannel.position: Long
    get() = try {
        channel.position()
    } catch (ce: ClosedChannelException) {
        throw SysException(BADF, cause = ce)
    } catch (ioe: IOException) {
        throw SysException(IO, cause = ioe)
    }
    set(newPosition) = try {
        channel.position(newPosition)
        Unit
    } catch (ce: ClosedChannelException) {
        throw SysException(BADF, cause = ce)
    } catch (ioe: IOException) {
        throw SysException(IO, cause = ioe)
    } catch (iae: IllegalArgumentException) {
        throw SysException(INVAL, "Negative new position", cause = iae)
    }

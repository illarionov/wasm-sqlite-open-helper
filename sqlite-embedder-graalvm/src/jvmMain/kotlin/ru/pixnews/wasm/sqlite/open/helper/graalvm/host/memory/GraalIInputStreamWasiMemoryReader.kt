/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.Channels
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonReadableChannelException

internal class GraalIInputStreamWasiMemoryReader(
    private val memory: WasmHostMemoryImpl,
) : WasiMemoryReader {
    private val wasmMemory = memory.memory
    private val defaultMemoryReader = DefaultWasiMemoryReader(memory)

    override fun read(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong {
        return if (strategy == CHANGE_POSITION) {
            read(channel, iovecs)
        } else {
            defaultMemoryReader.read(channel, strategy, iovecs)
        }
    }

    @Suppress("ThrowsCount")
    private fun read(
        channel: FdChannel,
        iovecs: IovecArray,
    ): ULong {
        var totalBytesRead: ULong = 0U
        try {
            for (vec in iovecs.iovecList) {
                val inputStream = Channels.newInputStream(channel.channel)
                val limit = vec.bufLen.value.toInt()
                val bytesRead = wasmMemory.copyFromStream(memory.node, inputStream, vec.buf.addr, limit)
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead.toULong()
                }
                if (bytesRead < limit) {
                    break
                }
            }
        } catch (cce: ClosedChannelException) {
            throw SysException(Errno.IO, "Channel closed", cce)
        } catch (ace: AsynchronousCloseException) {
            throw SysException(Errno.IO, "Channel closed on other thread", ace)
        } catch (ci: ClosedByInterruptException) {
            throw SysException(Errno.INTR, "Interrupted", ci)
        } catch (nre: NonReadableChannelException) {
            throw SysException(Errno.BADF, "Non readable channel", nre)
        } catch (ioe: IOException) {
            throw SysException(Errno.IO, "I/o error", ioe)
        }

        return totalBytesRead
    }
}

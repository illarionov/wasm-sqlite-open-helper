/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.memory.WasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.Channels
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonReadableChannelException

internal class GraalOutputStreamWasiMemoryWriter(
    private val memory: WasmHostMemoryImpl,
    logger: Logger,
) : WasiMemoryWriter {
    private val logger = logger.withTag("GraalOutputStreamWasiMemoryWriter")
    private val wasmMemory = memory.memory
    private val defaultMemoryWriter = DefaultWasiMemoryWriter(memory, logger)

    override fun write(channel: FdChannel, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        return if (strategy == ReadWriteStrategy.CHANGE_POSITION) {
            write(channel, cioVecs)
        } else {
            defaultMemoryWriter.write(channel, strategy, cioVecs)
        }
    }

    @Suppress("ThrowsCount")
    private fun write(channel: FdChannel, cioVecs: CiovecArray): ULong {
        logger.v { "write(${channel.fd}, ${cioVecs.ciovecList.map { it.bufLen.value }})" }
        var totalBytesWritten: ULong = 0U
        try {
            val outputStream = Channels.newOutputStream(channel.channel).buffered()
            for (vec in cioVecs.ciovecList) {
                val limit = vec.bufLen.value.toInt()
                wasmMemory.copyToStream(memory.node, outputStream, vec.buf.addr, limit)
                totalBytesWritten += limit.toUInt()
            }
            outputStream.flush()
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

        return totalBytesWritten
    }
}

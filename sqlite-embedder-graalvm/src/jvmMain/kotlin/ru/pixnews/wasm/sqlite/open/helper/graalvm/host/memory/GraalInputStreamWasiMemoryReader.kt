/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy.CHANGE_POSITION
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.JvmFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.io.IOException
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.Channels
import java.nio.channels.ClosedByInterruptException
import java.nio.channels.ClosedChannelException
import java.nio.channels.NonReadableChannelException
import kotlin.time.measureTimedValue

internal class GraalInputStreamWasiMemoryReader(
    private val memory: GraalvmWasmHostMemoryAdapter,
    private val fileSystem: JvmFileSystem,
    logger: Logger,
) : WasiMemoryReader {
    private val logger: Logger = logger.withTag("GraalInputStreamWasiMemoryReader")
    private val wasmMemory get() = memory.wasmMemory
    private val defaultMemoryReader = DefaultWasiMemoryReader(memory, fileSystem, logger)

    override fun read(
        fd: Fd,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong {
        val bytesRead = measureTimedValue {
            if (strategy == CHANGE_POSITION) {
                read(fd, iovecs)
            } else {
                defaultMemoryReader.read(fd, strategy, iovecs)
            }
        }
        logger.v {
            "read($fd, $strategy, ${iovecs.iovecList.map { it.bufLen.value }}): " +
                    "${bytesRead.value} in ${bytesRead.duration}"
        }
        return bytesRead.value
    }

    @Suppress("ThrowsCount")
    private fun read(
        fd: Fd,
        iovecs: IovecArray,
    ): ULong {
        var totalBytesRead: ULong = 0U
        try {
            val channel = fileSystem.getNioFileChannelByFd(fd)
            val inputStream = Channels.newInputStream(channel).buffered()
            for (vec in iovecs.iovecList) {
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

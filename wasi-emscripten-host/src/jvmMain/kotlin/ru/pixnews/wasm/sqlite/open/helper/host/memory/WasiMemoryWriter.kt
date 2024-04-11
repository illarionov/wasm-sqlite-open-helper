/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.memory

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import java.nio.ByteBuffer

public fun interface WasiMemoryWriter {
    public fun write(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        cioVecs: CiovecArray,
    ): ULong
}

public class DefaultWasiMemoryWriter(
    private val memory: Memory,
    logger: Logger,
) : WasiMemoryWriter {
    private val logger: Logger = logger.withTag("DefaultWasiMemoryWriter")
    override fun write(channel: FdChannel, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        logger.v { "write(${channel.fd}, ${cioVecs.ciovecList.map { it.bufLen.value }})" }
        val bufs = cioVecs.toByteBuffers(memory)
        return channel.fileSystem.write(channel, bufs, strategy)
    }

    private fun CiovecArray.toByteBuffers(
        memory: Memory,
    ): Array<ByteBuffer> = Array(ciovecList.size) { idx ->
        val ciovec = ciovecList[idx]
        val bytes = memory.readBytes(
            ciovec.buf,
            ciovec.bufLen.value.toInt(),
        )
        ByteBuffer.wrap(bytes)
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.memory

import ru.pixnews.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.sqlite.open.helper.host.filesystem.fd.FdChannel
import ru.pixnews.sqlite.open.helper.host.wasi.preview1.type.IovecArray
import java.nio.ByteBuffer

public fun interface WasiMemoryReader {
    public fun read(
        channel: FdChannel,
        strategy: ReadWriteStrategy,
        iovecs: IovecArray,
    ): ULong
}

public class DefaultWasiMemoryReader(
    private val memory: Memory,
) : WasiMemoryReader {
    override fun read(channel: FdChannel, strategy: ReadWriteStrategy, iovecs: IovecArray): ULong {
        val bbufs = iovecs.toByteBuffers()
        val readBytes = channel.fileSystem.read(channel, bbufs, strategy)
        iovecs.iovecList.forEachIndexed { idx, vec ->
            val bbuf: ByteBuffer = bbufs[idx]
            bbuf.flip()
            if (bbuf.limit() != 0) {
                require(bbuf.hasArray())
                memory.write(
                    vec.buf,
                    bbuf.array(),
                    0,
                    bbuf.limit(),
                )
            }
        }
        return readBytes
    }

    private fun IovecArray.toByteBuffers(): Array<ByteBuffer> = Array(iovecList.size) {
        ByteBuffer.allocate(iovecList[it].bufLen.value.toInt())
    }
}

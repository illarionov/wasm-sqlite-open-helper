/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.fold
import com.github.michaelbull.result.getOrThrow
import io.github.charlietap.chasm.embedding.memory.readMemory
import io.github.charlietap.chasm.embedding.memory.writeMemory
import io.github.charlietap.chasm.executor.memory.grow.MemoryGrowerImpl
import io.github.charlietap.chasm.executor.memory.read.MemoryInstanceIntReaderImpl
import io.github.charlietap.chasm.executor.memory.read.MemoryInstanceLongReaderImpl
import io.github.charlietap.chasm.executor.memory.write.MemoryInstanceIntWriterImpl
import io.github.charlietap.chasm.executor.memory.write.MemoryInstanceLongWriterImpl
import io.github.charlietap.chasm.executor.runtime.error.ModuleRuntimeError
import io.github.charlietap.chasm.executor.runtime.ext.memory
import io.github.charlietap.chasm.executor.runtime.store.Address
import io.github.charlietap.chasm.executor.runtime.store.Store
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.orThrow
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.exception.ChasmModuleRuntimeErrorException
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryReader
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.DefaultWasiMemoryWriter
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.ReadWriteStrategy
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.CiovecArray
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Fd
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.IovecArray

internal class ChasmMemoryAdapter(
    private val store: Store,
    private val memoryAddress: Address.Memory,
    fileSystem: FileSystem<*>,
    logger: Logger,
) : Memory {
    private val memoryReader = DefaultWasiMemoryReader(this, fileSystem, logger)
    private val memoryWriter = DefaultWasiMemoryWriter(this, fileSystem, logger)

    val memoryInstance get() = store.memory(memoryAddress).getOrThrow { ChasmModuleRuntimeErrorException(it) }

    override fun readI8(addr: WasmPtr<*>): Byte {
        return readMemory(store, memoryAddress, addr.addr).orThrow()
    }

    override fun readI32(addr: WasmPtr<*>): Int {
        return MemoryInstanceIntReaderImpl(memoryInstance, addr.addr, 4).getOrThrow()
    }

    override fun readI64(addr: WasmPtr<*>): Long {
        return MemoryInstanceLongReaderImpl(memoryInstance, addr.addr, 8).getOrThrow()
    }

    override fun readBytes(addr: WasmPtr<*>, length: Int): ByteArray {
        return ByteArray(length) { readMemory(store, memoryAddress, addr.addr + it).orThrow() }
    }

    override fun writeByte(addr: WasmPtr<*>, data: Byte) {
        writeMemory(store, memoryAddress, addr.addr, data).orThrow()
    }

    override fun writeI32(addr: WasmPtr<*>, data: Int) {
        return MemoryInstanceIntWriterImpl(memoryInstance, data, addr.addr, 4).getOrThrow()
    }

    override fun writeI64(addr: WasmPtr<*>, data: Long) {
        return MemoryInstanceLongWriterImpl(memoryInstance, data, addr.addr, 8).getOrThrow()
    }

    override fun write(addr: WasmPtr<*>, data: ByteArray, offset: Int, size: Int) {
        for (addrOffset in 0 until size) {
            writeMemory(store, memoryAddress, addr.addr + addrOffset, data[offset + addrOffset])
        }
    }

    override fun readFromChannel(fd: Fd, strategy: ReadWriteStrategy, iovecs: IovecArray): ULong {
        return memoryReader.read(fd, strategy, iovecs)
    }

    override fun writeToChannel(fd: Fd, strategy: ReadWriteStrategy, cioVecs: CiovecArray): ULong {
        return memoryWriter.write(fd, strategy, cioVecs)
    }

    // XXX
    fun grow(pagesToAdd: Int): Int {
        val oldPages = memoryInstance.data.min.amount
        return MemoryGrowerImpl(memoryInstance, pagesToAdd).fold(
            { newMemoryInstance ->
                store.memories[memoryAddress.address] = newMemoryInstance
                oldPages
            },
        ) {
            -1
        }
    }
}

private fun <T : Any, E : ModuleRuntimeError> Result<T, E>.getOrThrow(): T = getOrThrow {
    ChasmModuleRuntimeErrorException(it)
}

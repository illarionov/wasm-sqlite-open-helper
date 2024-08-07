/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory

import com.oracle.truffle.api.nodes.Node
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory

/**
 * [Memory] implementation based on GraalVM [WasmMemory]
 */
internal class GraalvmWasmHostMemoryAdapter(
    private val memoryProvider: () -> WasmMemory,
    internal val node: Node?,
) : Memory {
    internal val wasmMemory: WasmMemory get() = memoryProvider.invoke()

    constructor(
        memoryModuleInstance: WasmInstance,
        node: Node?,
    ) : this({ memoryModuleInstance.memory(0) }, node)

    override fun readI8(addr: WasmPtr<*>): Byte {
        return wasmMemory.load_i32_8u(node, addr.addr.toLong()).toByte()
    }

    override fun readI32(addr: WasmPtr<*>): Int {
        return wasmMemory.load_i32(node, addr.addr.toLong())
    }

    override fun readI64(addr: WasmPtr<*>): Long {
        return wasmMemory.load_i64(node, addr.addr.toLong())
    }

    override fun source(fromAddr: WasmPtr<*>, toAddrExclusive: WasmPtr<*>): RawSource {
        return GraalvmMemoryRawSource(memoryProvider, fromAddr, toAddrExclusive, node)
    }

    override fun writeI8(addr: WasmPtr<*>, data: Byte) {
        wasmMemory.store_i32_8(node, addr.addr.toLong(), data)
    }

    override fun writeI32(addr: WasmPtr<*>, data: Int) {
        wasmMemory.store_i32(node, addr.addr.toLong(), data)
    }

    override fun writeI64(addr: WasmPtr<*>, data: Long) {
        wasmMemory.store_i64(node, addr.addr.toLong(), data)
    }

    override fun sink(fromAddr: WasmPtr<*>, toAddrExclusive: WasmPtr<*>): RawSink {
        return GraalvmMemoryRawSink(memoryProvider, fromAddr, toAddrExclusive)
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func

import com.oracle.truffle.api.CompilerDirectives
import com.oracle.truffle.api.frame.VirtualFrame
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.getArgAsInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.BaseWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

internal class EmscriptenResizeHeap(
    language: WasmLanguage,
    module: WasmModule,
    host: SqliteEmbedderHost,
    functionName: String = "emscripten_resize_heap",
) : BaseWasmNode(language, module, host, functionName) {
    private val logger = host.rootLogger.withTag(EmscriptenResizeHeap::class.qualifiedName!!)

    override fun executeWithContext(frame: VirtualFrame, context: WasmContext, instance: WasmInstance): Any {
        return emscriptenResizeheap(
            memory(frame),
            frame.arguments.getArgAsInt(0).toLong(),
        )
    }

    @CompilerDirectives.TruffleBoundary
    @Suppress("MemberNameEqualsClassName")
    private fun emscriptenResizeheap(
        memory: WasmMemory,
        requestedSize: Long,
    ): Int = try {
        val currentPages = memory.size()
        val declaredMaxPages = memory.declaredMaxSize()
        val newSizePages = calculateNewSizePages(requestedSize, currentPages, declaredMaxPages)

        logger.v {
            "emscripten_resize_heap($requestedSize). " +
                    "Requested: ${newSizePages * PAGE_SIZE} bytes ($newSizePages pages)"
        }

        val memoryAdded = memory.grow(newSizePages - currentPages)
        if (!memoryAdded) {
            throw SysException(
                Errno.NOMEM,
                "Cannot enlarge memory, requested $newSizePages pages, but the limit is " +
                        "${memory.declaredMaxSize()} pages!",
            )
        }
        1
    } catch (e: SysException) {
        -e.errNo.code
    }

    @Suppress("MagicNumber")
    companion object {
        const val PAGE_SIZE = 65536

        // XXX: copy of chicory version
        fun calculateNewSizePages(
            requestedSizeBytes: Long,
            memoryPages: Long,
            memoryMaxPages: Long,
        ): Long {
            check(requestedSizeBytes > memoryPages * PAGE_SIZE)

            val oldSize = memoryPages * PAGE_SIZE
            val overGrownHeapSize = minOf(
                oldSize + (oldSize / 5),
                requestedSizeBytes + 100_663_296L,
            ).coerceAtLeast(requestedSizeBytes)
            return ((overGrownHeapSize + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtMost(memoryMaxPages)
        }
    }
}

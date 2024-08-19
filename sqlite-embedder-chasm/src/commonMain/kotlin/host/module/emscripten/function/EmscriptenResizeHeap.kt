/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function

import io.github.charlietap.chasm.executor.runtime.value.ExecutionValue
import io.github.charlietap.chasm.executor.runtime.value.NumberValue.I32
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.asInt
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory.ChasmMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.EmscriptenHostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Pages
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_32_MAX_PAGES
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function.EmscriptenResizeHeapFunctionHandle.Companion.calculateNewSizePages
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.Errno.NOMEM

internal class EmscriptenResizeHeap(
    host: EmbedderHost,
    private val memory: ChasmMemoryAdapter,
) : EmscriptenHostFunctionHandle {
    private val logger: Logger = host.rootLogger.withTag("wasm-func:emscripten_resize_heap")

    override fun invoke(args: List<ExecutionValue>): List<ExecutionValue> {
        val requestedSize = args[0].asInt().toLong()

        val chasmMemoryLimits = memory.memoryInstance.data
        val oldPages = Pages(chasmMemoryLimits.min.amount.toLong())
        val maxPages = chasmMemoryLimits.max?.amount?.toLong()?.let(::Pages) ?: WASM_MEMORY_32_MAX_PAGES
        val newSizePages = calculateNewSizePages(requestedSize, oldPages, maxPages)

        logger.v {
            "emscripten_resize_heap($requestedSize). " +
                    "Requested: ${newSizePages.inBytes} bytes ($newSizePages pages)"
        }

        val prevPages = memory.grow((newSizePages.count - oldPages.count).toInt())
        if (prevPages < 0) {
            logger.e {
                "Cannot enlarge memory, requested $newSizePages pages, but the limit is " +
                        "$maxPages pages!"
            }
            return listOf(I32(-NOMEM.code))
        }
        return listOf(I32(1))
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.function.HostFunctionHandle
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Pages
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_PAGE_SIZE
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

public class EmscriptenResizeHeapFunctionHandle(
    host: EmbedderHost,
) : HostFunctionHandle(EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP, host) {
    @Suppress("MagicNumber")
    public companion object {
        private const val OVER_GROWN_HEAP_SIZE_MAX_ADD = 96 * 1024 * 1024

        public fun calculateNewSizePages(
            requestedSizeBytes: Long,
            memoryPages: Pages,
            memoryMaxPages: Pages,
        ): Pages {
            check(requestedSizeBytes > memoryPages.inBytes)

            val oldSize = memoryPages.inBytes
            val overGrownHeapSize = minOf(
                oldSize + (oldSize / 5),
                requestedSizeBytes + OVER_GROWN_HEAP_SIZE_MAX_ADD,
            ).coerceAtLeast(requestedSizeBytes)
            val newPages = ((overGrownHeapSize + WASM_MEMORY_PAGE_SIZE - 1) / WASM_MEMORY_PAGE_SIZE)
                .coerceAtMost(memoryMaxPages.count)
            return Pages(newPages)
        }
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Memory.PAGE_SIZE
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.emscriptenEnvHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.SysException
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import java.util.logging.Logger

internal fun emscriptenResizeHeap(
    moduleName: String = ENV_MODULE_NAME,
): HostFunction = emscriptenEnvHostFunction(
    funcName = "emscripten_resize_heap",
    paramTypes = listOf(
        I32, // requestedSize
    ),
    returnType = I32,
    moduleName = moduleName,
    handle = EmscriptenResizeHeap(),
)

private class EmscriptenResizeHeap(
    private val logger: Logger = Logger.getLogger(EmscriptenResizeHeap::class.qualifiedName),
) : EmscriptenHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Value {
        val memory = instance.memory()
        val requestedSize = args[0].asInt().toLong()

        val newSizePages = calculateNewSizePages(
            requestedSize,
            memory.pages().toLong(),
            memory.maximumPages().toLong(),
        )

        logger.finest {
            "emscripten_resize_heap($requestedSize). " +
                    "Requested: ${newSizePages * PAGE_SIZE} bytes ($newSizePages pages)"
        }

        val prevPages = memory.grow((newSizePages - memory.pages()).toInt())
        if (prevPages < 0) {
            throw SysException(
                Errno.NOMEM,
                "Cannot enlarge memory, requested $newSizePages pages, but the limit is " +
                        "${memory.maximumPages()} pages!",
            )
        }
        return Value.i32(1)
    }

    companion object {
        private const val OVER_GROWN_HEAP_SIZE_MAX_ADD = 96 * 1024 * 1024

        fun calculateNewSizePages(
            requestedSizeBytes: Long,
            memoryPages: Long,
            memoryMaxPages: Long,
        ): Long {
            check(requestedSizeBytes > memoryPages * PAGE_SIZE)

            val oldSize = memoryPages * PAGE_SIZE

            @Suppress("MagicNumber")
            val overGrownHeapSize = minOf(
                oldSize + (oldSize / 5),
                requestedSizeBytes + OVER_GROWN_HEAP_SIZE_MAX_ADD,
            ).coerceAtLeast(requestedSizeBytes)
            return ((overGrownHeapSize + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtMost(memoryMaxPages)
        }
    }
}

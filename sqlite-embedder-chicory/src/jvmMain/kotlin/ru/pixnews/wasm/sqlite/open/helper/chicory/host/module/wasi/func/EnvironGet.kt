/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.asWasmAddr
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.wasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.pointer
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.ext.WasiEnvironmentFunc
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiValueTypes.U8

/**
 * Read environment variable data.
 * The sizes of the buffers should match that returned by `environ_sizes_get`.
 * Key/value pairs are expected to be joined with `=`s, and terminated with `\0`s.
 *
 * (@interface func (export "environ_get")
 *     (param $environ (@witx pointer (@witx pointer u8)))
 *     (param $environ_buf (@witx pointer u8))
 *     (result $error (expected (error $errno)))
 *   )
 */
@Suppress("LAMBDA_IS_NOT_LAST_PARAMETER")
internal fun environGet(
    memory: Memory,
    envProvider: () -> Map<String, String> = System::getenv,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "environ_get",
    paramTypes = listOf(
        U8.pointer, // **environ
        U8.pointer, // *environ_buf
    ),
    moduleName = moduleName,
) { _, params ->
    WasiEnvironmentFunc.environGet(
        envProvider = envProvider,
        memory = memory,
        environPAddr = params[0].asWasmAddr(),
        environBufAddr = params[1].asWasmAddr(),
    )
}

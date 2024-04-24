/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func

import com.dylibso.chicory.runtime.HostFunction
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WASI_SNAPSHOT_PREVIEW1
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.WasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.wasiHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno

@Suppress("UnusedParameter", "MagicNumber")
internal fun pathLink(
    filesystem: FileSystem,
    moduleName: String = WASI_SNAPSHOT_PREVIEW1,
): HostFunction = wasiHostFunction(
    funcName = "path_link",
    paramTypes = List(7) { I32 },
    moduleName = moduleName,
    handle = PathLink(),
)

private class PathLink : WasiHostFunction {
    override fun apply(instance: Instance, vararg args: Value): Errno {
        TODO("Not yet implemented")
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("NoMultipleSpaces")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.memory.ChicoryMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.abortFunc
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.assertFail
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.emscriptenDateNow
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.emscriptenGetNow
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.emscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.emscriptenResizeHeap
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.localtimeJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.mmapJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.munmapJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallChmod
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallFaccessat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallFchmod
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallFchown32
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallFcntl64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallFstat64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallFtruncate64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallGetcwd
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallIoctl
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallLstat64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallMkdirat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallNewfstatat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallOpenat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallReadlinkat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallRmdir
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallStat64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallUnlinkat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallUtimensat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.tzsetJs
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import java.time.Clock

internal const val ENV_MODULE_NAME = "env"

internal class EmscriptenEnvBindings(
    memory: ChicoryMemoryAdapter,
    filesystem: FileSystem,
    logger: Logger,
    clock: Clock = Clock.systemDefaultZone(),
) {
    val functions: List<HostFunction> = listOf(
        abortFunc(),
        assertFail(memory),
        emscriptenDateNow(clock),
        emscriptenGetNow(),
        emscriptenGetNowIsMonotonic(),
        emscriptenResizeHeap(),
        localtimeJs(),                  // Not yet implemented
        mmapJs(filesystem),             // Not yet implemented
        munmapJs(filesystem),           // Not yet implemented
        syscallChmod(filesystem),       // Not yet implemented
        syscallFaccessat(filesystem),   // Not yet implemented
        syscallFchmod(filesystem),      // Not yet implemented
        syscallFchown32(filesystem),
        syscallFcntl64(filesystem),     // Not yet implemented
        syscallFstat64(filesystem),
        syscallFtruncate64(filesystem), // Not yet implemented
        syscallGetcwd(filesystem),
        syscallIoctl(filesystem),       // Not yet implemented
        syscallLstat64(memory, filesystem),
        syscallMkdirat(filesystem),     // Not yet implemented
        syscallNewfstatat(filesystem),  // Not yet implemented
        syscallOpenat(memory, filesystem, logger),
        syscallReadlinkat(filesystem),  // Not yet implemented
        syscallRmdir(filesystem),       // Not yet implemented
        syscallStat64(memory, filesystem),
        syscallUnlinkat(memory, filesystem),
        syscallUtimensat(filesystem),   // Not yet implemented
        tzsetJs(),                      // Not yet implemented
    )
}

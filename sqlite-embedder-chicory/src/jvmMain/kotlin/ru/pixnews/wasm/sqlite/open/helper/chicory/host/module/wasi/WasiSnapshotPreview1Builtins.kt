/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("NoMultipleSpaces")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi

import com.dylibso.chicory.runtime.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.argsGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.argsSizesGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.clockTimeGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.environGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.environSizesGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdClose
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdFdstatGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdFdstatSetFlags
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdFilestatGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdPread
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdPrestatDirName
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdPrestatGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdPwrite
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdRead
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdSeek
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdSync
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.fdWrite
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathCreateDirectory
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathFilestatGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathFilestatSetTimes
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathLink
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathOpen
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathReadlink
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathRemoveDirectory
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathRename
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathSymlink
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.pathUnlinkFile
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.randomGet
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.wasi.func.schedYield
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import java.time.Clock

// https://github.com/WebAssembly/WASI/tree/main
internal class WasiSnapshotPreview1Builtins(
    memory: Memory,
    fileSystem: FileSystem,
    argsProvider: () -> List<String> = ::emptyList,
    envProvider: () -> Map<String, String> = System::getenv,
    clock: Clock = Clock.systemDefaultZone(),
) {
    val functions: List<HostFunction> = listOf(
        argsGet(argsProvider),            // Not yet implemented
        argsSizesGet(argsProvider),       // Not yet implemented
        clockTimeGet(clock),              // Not yet implemented
        environGet(memory, envProvider),
        environSizesGet(memory, envProvider),
        fdClose(fileSystem),
        fdFdstatGet(fileSystem),          // Not yet implemented
        fdFdstatSetFlags(fileSystem),     // Not yet implemented
        fdFilestatGet(fileSystem),        // Not yet implemented
        fdPread(memory, fileSystem),
        fdPrestatDirName(fileSystem),     // Not yet implemented
        fdPrestatGet(fileSystem),         // Not yet implemented
        fdPwrite(memory, fileSystem),
        fdRead(memory, fileSystem),
        fdSeek(memory, fileSystem),
        fdSync(fileSystem),
        fdWrite(memory, fileSystem),
        pathCreateDirectory(fileSystem),  // Not yet implemented
        pathFilestatGet(fileSystem),      // Not yet implemented
        pathFilestatSetTimes(fileSystem), // Not yet implemented
        pathLink(fileSystem),             // Not yet implemented
        pathOpen(fileSystem),             // Not yet implemented
        pathReadlink(fileSystem),         // Not yet implemented
        pathRemoveDirectory(fileSystem),  // Not yet implemented
        pathRename(fileSystem),           // Not yet implemented
        pathSymlink(fileSystem),          // Not yet implemented
        pathUnlinkFile(fileSystem),       // Not yet implemented
        randomGet(fileSystem),            // Not yet implemented
        schedYield(),                     // Not yet implemented
    )
}

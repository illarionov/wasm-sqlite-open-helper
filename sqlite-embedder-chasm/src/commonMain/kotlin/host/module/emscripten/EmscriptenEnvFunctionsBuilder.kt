/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten

import io.github.charlietap.chasm.embedding.function
import io.github.charlietap.chasm.executor.runtime.store.Store
import ru.pixnews.wasm.sqlite.open.helper.chasm.ext.toChasmFunctionTypes
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.memory.ChasmMemoryAdapter
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.AbortJs
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.AssertFail
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.EmscriptenConsoleError
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.EmscriptenDateNow
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.EmscriptenGetNow
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.EmscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.EmscriptenResizeHeap
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.Getentropy
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.LocaltimeJs
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.MmapJs
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.MunmapJs
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.NotImplementedEmscriptenFunction
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallChmod
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallFaccessat
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallFchmod
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallFchown32
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallFcntl64
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallFdatasync
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallFstat64
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallFtruncate64
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallGetcwd
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallMkdirat
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallOpenat
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallReadlinkat
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallRmdir
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallUnlinkat
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.SyscallUtimensat
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.TzsetJs
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.syscallLstat64
import ru.pixnews.wasm.sqlite.open.helper.chasm.host.module.emscripten.function.syscallStat64
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.ABORT_JS
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.ASSERT_FAIL
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.EMSCRIPTEN_CONSOLE_ERROR
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.EMSCRIPTEN_DATE_NOW
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.EMSCRIPTEN_GET_NOW
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.EMSCRIPTEN_GET_NOW_IS_MONOTONIC
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.GETENTROPY
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.LOCALTIME_JS
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.MMAP_JS
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.MUNMAP_JS
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_CHMOD
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_FACCESSAT
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_FCHMOD
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_FCHOWN32
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_FCNTL64
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_FDATASYNC
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_FSTAT64
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_FTRUNCATE64
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_GETCWD
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_LSTAT64
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_MKDIRAT
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_OPENAT
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_READLINKAT
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_RMDIR
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_STAT64
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_UNLINKAT
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.SYSCALL_UTIMENSAT
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction.TZSET_JS
import io.github.charlietap.chasm.import.Import as ChasmImport

internal fun getEmscriptenHostFunctions(
    store: Store,
    memory: ChasmMemoryAdapter,
    host: EmbedderHost,
    moduleName: String = ENV_MODULE_NAME,
): List<ChasmImport> {
    val functionTypes = EmscriptenHostFunction.entries.map(EmscriptenHostFunction::type).toChasmFunctionTypes()
    return EmscriptenHostFunction.entries.map { emscriptenFunc ->
        ChasmImport(
            moduleName = moduleName,
            entityName = emscriptenFunc.wasmName,
            value = function(
                store = store,
                type = functionTypes.getValue(emscriptenFunc.type),
                function = emscriptenFunc.createChasmHostFunction(host, memory),
            ),
        )
    }
}

@Suppress("CyclomaticComplexMethod")
private fun EmscriptenHostFunction.createChasmHostFunction(
    host: EmbedderHost,
    memory: ChasmMemoryAdapter,
): EmscriptenHostFunctionHandle = when (this) {
    ABORT_JS -> AbortJs(host)
    ASSERT_FAIL -> AssertFail(host, memory)
    EMSCRIPTEN_CONSOLE_ERROR -> EmscriptenConsoleError(host, memory)
    EMSCRIPTEN_DATE_NOW -> EmscriptenDateNow(host)
    EMSCRIPTEN_GET_NOW -> EmscriptenGetNow(host)
    EMSCRIPTEN_GET_NOW_IS_MONOTONIC -> EmscriptenGetNowIsMonotonic(host)
    EMSCRIPTEN_RESIZE_HEAP -> EmscriptenResizeHeap(host, memory)
    GETENTROPY -> Getentropy(host, memory)
    LOCALTIME_JS -> LocaltimeJs(host, memory)
    MMAP_JS -> MmapJs(host)
    MUNMAP_JS -> MunmapJs(host)
    SYSCALL_CHMOD -> SyscallChmod(host, memory)
    SYSCALL_FACCESSAT -> SyscallFaccessat(host, memory)
    SYSCALL_FCHMOD -> SyscallFchmod(host)
    SYSCALL_FCHOWN32 -> SyscallFchown32(host)
    SYSCALL_FCNTL64 -> SyscallFcntl64(host, memory)
    SYSCALL_FDATASYNC -> SyscallFdatasync(host)
    SYSCALL_FSTAT64 -> SyscallFstat64(host, memory)
    SYSCALL_FTRUNCATE64 -> SyscallFtruncate64(host)
    SYSCALL_GETCWD -> SyscallGetcwd(host, memory)
    SYSCALL_MKDIRAT -> SyscallMkdirat(host, memory)
    SYSCALL_OPENAT -> SyscallOpenat(host, memory)
    SYSCALL_RMDIR -> SyscallRmdir(host, memory)
    SYSCALL_READLINKAT -> SyscallReadlinkat(host, memory)
    SYSCALL_STAT64 -> syscallStat64(host, memory)
    SYSCALL_LSTAT64 -> syscallLstat64(host, memory)
    SYSCALL_UNLINKAT -> SyscallUnlinkat(host, memory)
    SYSCALL_UTIMENSAT -> SyscallUtimensat(host, memory)
    TZSET_JS -> TzsetJs(host, memory)
    else -> NotImplementedEmscriptenFunction(this)
}

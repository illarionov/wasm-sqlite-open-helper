/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("NoMultipleSpaces")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten

import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.WasmFunctionHandle
import com.dylibso.chicory.wasm.types.Value
import ru.pixnews.wasm.sqlite.open.helper.chicory.ext.chicory
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.Abort
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.AssertFail
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.EmscriptenDateNow
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.EmscriptenGetNow
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.EmscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.EmscriptenResizeHeap
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.LocaltimeJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.MmapJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.MunmapJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallChmod
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallFaccessat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallFchmod
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallFchown32
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallFcntl64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallFdatasync
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallFstat64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallFtruncate64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallGetcwd
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallMkdirat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallOpenat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallRmdir
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallUnlinkat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.SyscallUtimensat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.TzsetJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.notImplementedEmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallLstat64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.func.syscallStat64
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.memory.Memory
import com.dylibso.chicory.runtime.HostFunction as ChicoryHostFunction

internal class EmscriptenEnvFunctionsBuilder(
    private val memory: Memory,
    private val host: SqliteEmbedderHost,
) {
    fun asChicoryHostFunctions(
        moduleName: String = ENV_MODULE_NAME,
    ): List<ChicoryHostFunction> {
        return EmscriptenHostFunction.entries.map { emscriptenFunc ->
            ChicoryHostFunction(
                HostFunctionAdapter(emscriptenFunc.functionFactory(host, memory)),
                moduleName,
                emscriptenFunc.wasmName,
                emscriptenFunc.type.paramTypes.map(WasmValueType::chicory),
                emscriptenFunc.type.returnTypes.map(WasmValueType::chicory),
            )
        }
    }

    private class HostFunctionAdapter(
        private val delegate: EmscriptenHostFunctionHandle,
    ) : WasmFunctionHandle {
        override fun apply(instance: Instance, vararg args: Value): Array<Value> {
            val result: Value? = delegate.apply(instance, args = args)
            return if (result != null) {
                arrayOf(result)
            } else {
                arrayOf()
            }
        }
    }

    private companion object {
        const val ENV_MODULE_NAME = "env"

        private val EmscriptenHostFunction.functionFactory: EmscriptenHostFunctionHandleFactory
            get() = when (this) {
                EmscriptenHostFunction.ABORT -> ::Abort
                EmscriptenHostFunction.ASSERT_FAIL -> ::AssertFail
                EmscriptenHostFunction.EMSCRIPTEN_DATE_NOW -> ::EmscriptenDateNow
                EmscriptenHostFunction.EMSCRIPTEN_GET_NOW -> ::EmscriptenGetNow
                EmscriptenHostFunction.EMSCRIPTEN_GET_NOW_IS_MONOTONIC -> ::EmscriptenGetNowIsMonotonic
                EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP -> ::EmscriptenResizeHeap
                EmscriptenHostFunction.LOCALTIME_JS -> ::LocaltimeJs
                EmscriptenHostFunction.MMAP_JS -> ::MmapJs
                EmscriptenHostFunction.MUNMAP_JS -> ::MunmapJs
                EmscriptenHostFunction.SYSCALL_CHMOD -> ::SyscallChmod
                EmscriptenHostFunction.SYSCALL_FACCESSAT -> ::SyscallFaccessat
                EmscriptenHostFunction.SYSCALL_FCHMOD -> ::SyscallFchmod
                EmscriptenHostFunction.SYSCALL_FCHOWN32 -> ::SyscallFchown32
                EmscriptenHostFunction.SYSCALL_FCNTL64 -> ::SyscallFcntl64
                EmscriptenHostFunction.SYSCALL_FDATASYNC -> ::SyscallFdatasync
                EmscriptenHostFunction.SYSCALL_FSTAT64 -> ::SyscallFstat64
                EmscriptenHostFunction.SYSCALL_FTRUNCATE64 -> ::SyscallFtruncate64
                EmscriptenHostFunction.SYSCALL_GETCWD -> ::SyscallGetcwd
                EmscriptenHostFunction.SYSCALL_IOCTL -> notImplementedEmscriptenHostFunction
                EmscriptenHostFunction.SYSCALL_MKDIRAT -> ::SyscallMkdirat
                EmscriptenHostFunction.SYSCALL_NEWFSTATAT -> notImplementedEmscriptenHostFunction
                EmscriptenHostFunction.SYSCALL_OPENAT -> ::SyscallOpenat
                EmscriptenHostFunction.SYSCALL_READLINKAT -> notImplementedEmscriptenHostFunction
                EmscriptenHostFunction.SYSCALL_RMDIR -> ::SyscallRmdir
                EmscriptenHostFunction.SYSCALL_STAT64 -> ::syscallStat64
                EmscriptenHostFunction.SYSCALL_LSTAT64 -> ::syscallLstat64
                EmscriptenHostFunction.SYSCALL_UNLINKAT -> ::SyscallUnlinkat
                EmscriptenHostFunction.SYSCALL_UTIMENSAT -> ::SyscallUtimensat
                EmscriptenHostFunction.TZSET_JS -> ::TzsetJs
                EmscriptenHostFunction.EMSCRIPTEN_THREAD_SET_STRONGREF,
                EmscriptenHostFunction.EMSCRIPTEN_EXIT_WITH_LIVE_RUNTIME,
                EmscriptenHostFunction.EMSCRIPTEN_INIT_MAIN_THREAD_JS,
                EmscriptenHostFunction.EMSCRIPTEN_THREAD_MAILBOX_AWAIT,
                EmscriptenHostFunction.EMSCRIPTEN_RECEIVE_ON_MAIN_THREAD_JS,
                EmscriptenHostFunction.EMSCRIPTEN_CHECK_BLOCKING_ALLOWED,
                EmscriptenHostFunction.PTHREAD_CREATE_JS,
                EmscriptenHostFunction.EXIT,
                EmscriptenHostFunction.EMSCRIPTEN_THREAD_CLEANUP,
                EmscriptenHostFunction.EMSCRIPTEN_NOTIFY_MAILBOX_POSTMESSAGE,
                -> notImplementedEmscriptenHostFunction
            }
    }
}

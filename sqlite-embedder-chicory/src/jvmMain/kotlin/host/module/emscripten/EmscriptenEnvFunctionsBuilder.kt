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
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.AbortJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.AssertFail
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.EmscriptenConsoleError
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.EmscriptenDateNow
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.EmscriptenGetNow
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.EmscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.EmscriptenResizeHeap
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.Getentropy
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.LocaltimeJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.MmapJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.MunmapJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallChmod
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallFaccessat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallFchmod
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallFchown32
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallFcntl64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallFdatasync
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallFstat64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallFtruncate64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallGetcwd
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallMkdirat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallOpenat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallReadlinkat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallRmdir
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallUnlinkat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.SyscallUtimensat
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.TzsetJs
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.notImplementedEmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.syscallLstat64
import ru.pixnews.wasm.sqlite.open.helper.chicory.host.module.emscripten.function.syscallStat64
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Memory
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import com.dylibso.chicory.runtime.HostFunction as ChicoryHostFunction

internal class EmscriptenEnvFunctionsBuilder(
    private val memory: Memory,
    private val host: EmbedderHost,
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
                EmscriptenHostFunction.ABORT_JS -> ::AbortJs
                EmscriptenHostFunction.ASSERT_FAIL -> ::AssertFail
                EmscriptenHostFunction.EMSCRIPTEN_CONSOLE_ERROR -> ::EmscriptenConsoleError
                EmscriptenHostFunction.EMSCRIPTEN_DATE_NOW -> ::EmscriptenDateNow
                EmscriptenHostFunction.EMSCRIPTEN_GET_NOW -> ::EmscriptenGetNow
                EmscriptenHostFunction.EMSCRIPTEN_GET_NOW_IS_MONOTONIC -> ::EmscriptenGetNowIsMonotonic
                EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP -> ::EmscriptenResizeHeap
                EmscriptenHostFunction.GETENTROPY -> ::Getentropy
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
                EmscriptenHostFunction.SYSCALL_READLINKAT -> ::SyscallReadlinkat
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

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.constants.Sizes
import ru.pixnews.wasm.sqlite.open.helper.graalvm.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupWasmModuleFunctions
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.HostFunction
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.Abort
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.AssertFail
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenDateNow
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenGetNow
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenInitMainThreadJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenResizeHeap
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenThreadMailboxAwait
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFchown32
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFstat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFtruncate64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallGetcwd
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallMkdirat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallOpenat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallRmdir
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallUnlinkat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallUtimensat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.syscallLstat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.syscallStat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.fn
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.fnVoid
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.SharedMemoryWaiterListStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.WasmMemoryNotifyCallback
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.WasmMemoryWaitCallback
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.pthread.Pthread
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.F64
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType.WebAssemblyTypes.I64

internal class EmscriptenEnvModuleBuilder(
    private val graalContext: Context,
    private val host: SqliteEmbedderHost,
    private val pthreadRef: () -> Pthread,
    private val moduleName: String = ENV_MODULE_NAME,
) {
    private val envFunctions: List<HostFunction> = buildList {
        fnVoid(
            name = "abort",
            paramTypes = listOf(),
            nodeFactory = { language, module, _, functionName -> Abort(language, module, functionName) },
        )
        fnVoid(
            name = "__assert_fail",
            paramTypes = List(4) { I32 },
            nodeFactory = { language, module, _, functionName -> AssertFail(language, module, functionName) },
        )
        fn(
            name = "emscripten_date_now",
            paramTypes = listOf(),
            retType = F64,
            nodeFactory = ::EmscriptenDateNow,
        )
        fn(
            name = "emscripten_get_now",
            paramTypes = listOf(),
            retType = F64,
            nodeFactory = ::EmscriptenGetNow,
        )
        fn(
            name = "_emscripten_get_now_is_monotonic",
            paramTypes = listOf(),
            retType = I32,
            nodeFactory = ::EmscriptenGetNowIsMonotonic,
        )
        fn(
            name = "emscripten_resize_heap",
            paramTypes = listOf(I32),
            retType = I32,
            nodeFactory = ::EmscriptenResizeHeap,
        )
        fnVoid("_localtime_js", listOf(I64, I32))
        fn("_mmap_js", listOf(I32, I32, I32, I32, I64, I32, I32))
        fn("_munmap_js", listOf(I32, I32, I32, I32, I32, I64))
        // fnVoid("_localtime_js", listOf(I32, I32))
        // fn("_mmap_js", listOf(I32, I32, I32, I32, I32, I32, I32))
        // fn("_munmap_js", listOf(I32, I32, I32, I32, I32, I32))
        fn("__syscall_chmod", listOf(I32, I32))
        fn("__syscall_faccessat", List(4) { I32 })
        fn("__syscall_fchmod", listOf(I32, I32))
        fn(
            name = "__syscall_fchown32",
            paramTypes = List(3) { I32 },
            retType = I32,
            nodeFactory = ::SyscallFchown32,
        )
        fn("__syscall_fcntl64", List(3) { I32 })
        fn("__syscall_fstat64", listOf(I32, I32), I32, ::SyscallFstat64)
        fn("__syscall_ftruncate64", listOf(I32, I64), I32, ::SyscallFtruncate64)
        fn(
            name = "__syscall_getcwd",
            paramTypes = listOf(I32, I32),
            retType = I32,
            nodeFactory = ::SyscallGetcwd,
        )
        fn("__syscall_ioctl", List(3) { I32 })
        fn("__syscall_mkdirat", List(3) { I32 }, I32, ::SyscallMkdirat)
        fn("__syscall_newfstatat", List(4) { I32 })
        fn(
            name = "__syscall_openat",
            paramTypes = List(4) { I32 },
            retType = I32,
            nodeFactory = ::SyscallOpenat,
        )
        fn("__syscall_readlinkat", List(4) { I32 })
        fn("__syscall_rmdir", listOf(I32), I32, ::SyscallRmdir)
        fn(
            name = "__syscall_stat64",
            paramTypes = listOf(I32, I32),
            retType = I32,
            nodeFactory = ::syscallStat64,
        )
        fn(
            name = "__syscall_lstat64",
            paramTypes = listOf(I32, I32),
            retType = I32,
            nodeFactory = ::syscallLstat64,
        )
        fn(
            name = "__syscall_unlinkat",
            paramTypes = List(3) { I32 },
            retType = I32,
            nodeFactory = ::SyscallUnlinkat,
        )
        fn("__syscall_utimensat", List(4) { I32 }, I32, ::SyscallUtimensat)
        fnVoid("_tzset_js", List(4) { I32 })

        fnVoid("_emscripten_thread_set_strongref", listOf(I32))
        fnVoid("emscripten_exit_with_live_runtime", listOf())
        fnVoid(
            name = "__emscripten_init_main_thread_js",
            paramTypes = listOf(I32),
            nodeFactory = { language, module, host, functionName ->
                EmscriptenInitMainThreadJs(
                    language = language,
                    module = module,
                    functionName = functionName,
                    posixThreadRef = pthreadRef,
                )
            },
        )
        fnVoid(
            "_emscripten_thread_mailbox_await",
            listOf(I32),
            nodeFactory = { language, module, _, functionName ->
                EmscriptenThreadMailboxAwait(
                    language = language,
                    module = module,
                    functionName = functionName,
                    posixThreadRef = pthreadRef,
                    rootLogger = host.rootLogger,
                )
            },
        )
        fn("_emscripten_receive_on_main_thread_js", List(5) { I32 }, F64)
        fnVoid("emscripten_check_blocking_allowed", listOf())
        fn("__pthread_create_js", List(4) { I32 }, I32)
        fnVoid("exit", listOf(I32))
        fnVoid("__emscripten_thread_cleanup", listOf(I32))
        fnVoid("_emscripten_notify_mailbox_postmessage", listOf(I32, I32, I32))
    }

    fun setupModule(
        sharedMemory: Boolean = false,
        useUnsafeMemory: Boolean = false,
    ): WasmInstance = graalContext.withWasmContext { wasmContext ->
        val envModule = WasmModule.create(moduleName, null)
        setupMemory(wasmContext, envModule, sharedMemory, useUnsafeMemory)
        return setupWasmModuleFunctions(wasmContext, host, envModule, envFunctions)
    }

    @Suppress("MagicNumber", "LOCAL_VARIABLE_EARLY_DECLARATION")
    private fun setupMemory(
        context: WasmContext,
        envModule: WasmModule,
        sharedMemory: Boolean = false,
        useUnsafeMemory: Boolean = false,
    ) {
        val minSize = 256L
        val maxSize: Long
        val is64Bit: Boolean
        if (context.contextOptions.supportMemory64()) {
            maxSize = Sizes.MAX_MEMORY_64_DECLARATION_SIZE
            is64Bit = true
        } else {
            maxSize = 32768
            is64Bit = false
        }

        envModule.symbolTable().apply {
            val memoryIndex = memoryCount()
            allocateMemory(memoryIndex, minSize, maxSize, is64Bit, sharedMemory, false, useUnsafeMemory)
            exportMemory(memoryIndex, "memory")
        }
        val memoryWaiters = SharedMemoryWaiterListStore()
        envModule.addLinkAction { _: WasmContext, instance: WasmInstance ->
            instance.memory(0).apply {
                waitCallback = WasmMemoryWaitCallback(memoryWaiters, host.rootLogger)
                notifyCallback = WasmMemoryNotifyCallback(memoryWaiters, host.rootLogger)
            }
        }
    }

    companion object {
        private const val ENV_MODULE_NAME = "env"
    }
}

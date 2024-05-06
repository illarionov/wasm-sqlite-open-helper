/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupWasmModuleFunctions
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.NodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.Abort
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.AssertFail
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenDateNow
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenGetNow
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenInitMainThreadJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenResizeHeap
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.EmscriptenThreadMailboxAwait
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.LocaltimeJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.MmapJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.MunapJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallChmod
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFaccessat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFchmod
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFchown32
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFcntl64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFstat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallFtruncate64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallGetcwd
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallMkdirat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallOpenat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallRmdir
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallUnlinkat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.SyscallUtimensat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.TzsetJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.syscallLstat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.func.syscallStat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.SharedMemoryWaiterListStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.WasmMemoryNotifyCallback
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.WasmMemoryWaitCallback
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.notImplementedFunctionNodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.func.SyscallFdatasync
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.pthread.Pthread
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmSizes
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction

internal class EmscriptenEnvModuleBuilder(
    private val graalContext: Context,
    private val host: SqliteEmbedderHost,
    private val pthreadRef: () -> Pthread,
    private val moduleName: String = ENV_MODULE_NAME,
) {
    private val EmscriptenHostFunction.nodeFactory: NodeFactory
        get() = when (this) {
            EmscriptenHostFunction.ABORT -> ::Abort
            EmscriptenHostFunction.ASSERT_FAIL -> ::AssertFail
            EmscriptenHostFunction.EMSCRIPTEN_DATE_NOW -> ::EmscriptenDateNow
            EmscriptenHostFunction.EMSCRIPTEN_GET_NOW -> ::EmscriptenGetNow
            EmscriptenHostFunction.EMSCRIPTEN_GET_NOW_IS_MONOTONIC -> {
                    language: WasmLanguage,
                    module: WasmModule,
                    host: SqliteEmbedderHost,
                    functionName: String,
                ->
                EmscriptenGetNowIsMonotonic(
                    language = language,
                    module = module,
                    host = host,
                    functionName = functionName,
                )
            }

            EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP -> ::EmscriptenResizeHeap
            EmscriptenHostFunction.LOCALTIME_JS -> ::LocaltimeJs
            EmscriptenHostFunction.MMAP_JS -> ::MmapJs
            EmscriptenHostFunction.MUNMAP_JS -> ::MunapJs
            EmscriptenHostFunction.SYSCALL_CHMOD -> ::SyscallChmod
            EmscriptenHostFunction.SYSCALL_FACCESSAT -> ::SyscallFaccessat
            EmscriptenHostFunction.SYSCALL_FCHMOD -> ::SyscallFchmod
            EmscriptenHostFunction.SYSCALL_FCHOWN32 -> ::SyscallFchown32
            EmscriptenHostFunction.SYSCALL_FCNTL64 -> ::SyscallFcntl64
            EmscriptenHostFunction.SYSCALL_FDATASYNC -> ::SyscallFdatasync
            EmscriptenHostFunction.SYSCALL_FSTAT64 -> ::SyscallFstat64
            EmscriptenHostFunction.SYSCALL_FTRUNCATE64 -> ::SyscallFtruncate64
            EmscriptenHostFunction.SYSCALL_GETCWD -> ::SyscallGetcwd
            EmscriptenHostFunction.SYSCALL_IOCTL -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.SYSCALL_MKDIRAT -> ::SyscallMkdirat
            EmscriptenHostFunction.SYSCALL_NEWFSTATAT -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.SYSCALL_OPENAT -> ::SyscallOpenat
            EmscriptenHostFunction.SYSCALL_READLINKAT -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.SYSCALL_RMDIR -> ::SyscallRmdir
            EmscriptenHostFunction.SYSCALL_STAT64 -> ::syscallStat64
            EmscriptenHostFunction.SYSCALL_LSTAT64 -> ::syscallLstat64
            EmscriptenHostFunction.SYSCALL_UNLINKAT -> ::SyscallUnlinkat
            EmscriptenHostFunction.SYSCALL_UTIMENSAT -> ::SyscallUtimensat
            EmscriptenHostFunction.TZSET_JS -> ::TzsetJs
            EmscriptenHostFunction.EMSCRIPTEN_THREAD_SET_STRONGREF -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.EMSCRIPTEN_EXIT_WITH_LIVE_RUNTIME -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.EMSCRIPTEN_INIT_MAIN_THREAD_JS -> {
                    language: WasmLanguage,
                    module: WasmModule,
                    host: SqliteEmbedderHost,
                    functionName: String,
                ->
                EmscriptenInitMainThreadJs(
                    language = language,
                    module = module,
                    host = host,
                    functionName = functionName,
                    posixThreadRef = pthreadRef,
                )
            }

            EmscriptenHostFunction.EMSCRIPTEN_THREAD_MAILBOX_AWAIT -> {
                    language: WasmLanguage,
                    module: WasmModule,
                    host: SqliteEmbedderHost,
                    functionName: String,
                ->
                EmscriptenThreadMailboxAwait(
                    language = language,
                    module = module,
                    host = host,
                    functionName = functionName,
                    posixThreadRef = pthreadRef,
                )
            }

            EmscriptenHostFunction.EMSCRIPTEN_RECEIVE_ON_MAIN_THREAD_JS -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.EMSCRIPTEN_CHECK_BLOCKING_ALLOWED -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.PTHREAD_CREATE_JS -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.EXIT -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.EMSCRIPTEN_THREAD_CLEANUP -> notImplementedFunctionNodeFactory
            EmscriptenHostFunction.EMSCRIPTEN_NOTIFY_MAILBOX_POSTMESSAGE -> notImplementedFunctionNodeFactory
        }

    fun setupModule(
        minMemorySize: Long = 50331648L,
        sharedMemory: Boolean = false,
        useUnsafeMemory: Boolean = false,
    ): WasmInstance = graalContext.withWasmContext { wasmContext ->
        val envModule = WasmModule.create(moduleName, null)
        setupMemory(wasmContext, envModule, minMemorySize, sharedMemory, useUnsafeMemory)
        return setupWasmModuleFunctions(
            wasmContext,
            host,
            envModule,
            EmscriptenHostFunction.entries.associateWith { it.nodeFactory },
        )
    }

    @Suppress("MagicNumber", "LOCAL_VARIABLE_EARLY_DECLARATION")
    private fun setupMemory(
        context: WasmContext,
        envModule: WasmModule,
        minMemorySize: Long = 50_331_648L,
        sharedMemory: Boolean = false,
        useUnsafeMemory: Boolean = false,
    ) {
        val minSize = minMemorySize / WasmSizes.WASM_MEMORY_PAGE_SIZE
        val maxSize: Long
        val is64Bit: Boolean
        if (context.contextOptions.supportMemory64()) {
            maxSize = WasmSizes.WASM_MEMORY_64_MAX_PAGES
            is64Bit = true
        } else {
            maxSize = WasmSizes.WASM_MEMORY_SQLITE_MAX_PAGES
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
}

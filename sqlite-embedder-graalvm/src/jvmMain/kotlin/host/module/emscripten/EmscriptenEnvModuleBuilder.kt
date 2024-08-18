/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten

import org.graalvm.polyglot.Context
import org.graalvm.wasm.WasmContext
import org.graalvm.wasm.WasmInstance
import org.graalvm.wasm.WasmLanguage
import org.graalvm.wasm.WasmModule
import org.graalvm.wasm.memory.WasmMemory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.GraalvmSqliteRuntimeInternal
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.setupWasmModuleFunctions
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.SharedMemoryWaiterListStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.WasmMemoryNotifyCallback
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.memory.WasmMemoryWaitCallback
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.NodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.AbortJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.AssertFail
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenAsmConstAsyncOnMainThread
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenAsmConstInt
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenConsoleError
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenDateNow
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenGetNow
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenGetNowIsMonotonic
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenInitMainThreadJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenResizeHeap
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.EmscriptenThreadMailboxAwait
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.Getentropy
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.HandleStackOverflow
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.LocaltimeJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.MmapJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.MunapJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallChmod
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallFaccessat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallFchmod
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallFchown32
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallFcntl64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallFstat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallFtruncate64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallGetcwd
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallMkdirat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallOpenat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallReadlinkat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallRmdir
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallUnlinkat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.SyscallUtimensat
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.TzsetJs
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.syscallLstat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.emscripten.function.syscallStat64
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.notImplementedFunctionNodeFactory
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.wasi.function.SyscallFdatasync
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.GraalvmPthreadManager
import ru.pixnews.wasm.sqlite.open.helper.graalvm.pthread.PthreadCreateJsWasmNode
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmModules.ENV_MODULE_NAME
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.Pages
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_64_MAX_PAGES
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_PAGE_SIZE
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.WASM_MEMORY_SQLITE_MAX_PAGES
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.EmscriptenHostFunction
import ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.stack.EmscriptenStack

internal class EmscriptenEnvModuleBuilder(
    private val host: EmbedderHost,
    private val pthreadRef: () -> GraalvmPthreadManager,
    private val emscriptenStackRef: () -> EmscriptenStack,
    private val memoryWaiters: SharedMemoryWaiterListStore,
    private val moduleName: String = ENV_MODULE_NAME,
) {
    private val EmscriptenHostFunction.nodeFactory: NodeFactory
        get() = when (this) {
            EmscriptenHostFunction.ABORT_JS -> ::AbortJs
            EmscriptenHostFunction.ASSERT_FAIL -> ::AssertFail
            EmscriptenHostFunction.EMSCRIPTEN_ASM_CONST_INT -> ::EmscriptenAsmConstInt
            EmscriptenHostFunction.EMSCRIPTEN_ASM_CONST_ASYNC_ON_MAIN_THREAD -> ::EmscriptenAsmConstAsyncOnMainThread
            EmscriptenHostFunction.EMSCRIPTEN_DATE_NOW -> ::EmscriptenDateNow
            EmscriptenHostFunction.EMSCRIPTEN_CONSOLE_ERROR -> ::EmscriptenConsoleError
            EmscriptenHostFunction.EMSCRIPTEN_GET_NOW -> ::EmscriptenGetNow
            EmscriptenHostFunction.EMSCRIPTEN_GET_NOW_IS_MONOTONIC -> ::EmscriptenGetNowIsMonotonic
            EmscriptenHostFunction.EMSCRIPTEN_RESIZE_HEAP -> ::EmscriptenResizeHeap
            EmscriptenHostFunction.GETENTROPY -> ::Getentropy
            EmscriptenHostFunction.HANDLE_STACK_OVERFLOW -> {
                    language: WasmLanguage,
                    module: WasmModule,
                    host: EmbedderHost,
                ->
                HandleStackOverflow(
                    language = language,
                    module = module,
                    host = host,
                    stackBindingsRef = emscriptenStackRef,
                )
            }
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
            EmscriptenHostFunction.SYSCALL_IOCTL -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.SYSCALL_MKDIRAT -> ::SyscallMkdirat
            EmscriptenHostFunction.SYSCALL_NEWFSTATAT -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.SYSCALL_OPENAT -> ::SyscallOpenat
            EmscriptenHostFunction.SYSCALL_READLINKAT -> ::SyscallReadlinkat
            EmscriptenHostFunction.SYSCALL_RMDIR -> ::SyscallRmdir
            EmscriptenHostFunction.SYSCALL_STAT64 -> ::syscallStat64
            EmscriptenHostFunction.SYSCALL_LSTAT64 -> ::syscallLstat64
            EmscriptenHostFunction.SYSCALL_UNLINKAT -> ::SyscallUnlinkat
            EmscriptenHostFunction.SYSCALL_UTIMENSAT -> ::SyscallUtimensat
            EmscriptenHostFunction.TZSET_JS -> ::TzsetJs
            EmscriptenHostFunction.EMSCRIPTEN_THREAD_SET_STRONGREF -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.EMSCRIPTEN_UNWIND_TO_JS_EVENT_LOOP -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.EMSCRIPTEN_EXIT_WITH_LIVE_RUNTIME -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.EMSCRIPTEN_INIT_MAIN_THREAD_JS -> {
                    language: WasmLanguage,
                    module: WasmModule,
                    host: EmbedderHost,
                ->
                EmscriptenInitMainThreadJs(
                    language = language,
                    module = module,
                    host = host,
                    posixThreadRef = pthreadRef,
                )
            }

            EmscriptenHostFunction.EMSCRIPTEN_THREAD_MAILBOX_AWAIT -> {
                    language: WasmLanguage,
                    module: WasmModule,
                    host: EmbedderHost,
                ->
                EmscriptenThreadMailboxAwait(
                    language = language,
                    module = module,
                    host = host,
                    posixThreadRef = pthreadRef,
                )
            }

            EmscriptenHostFunction.EMSCRIPTEN_RECEIVE_ON_MAIN_THREAD_JS -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.EMSCRIPTEN_CHECK_BLOCKING_ALLOWED -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.PTHREAD_CREATE_JS -> {
                    language: WasmLanguage,
                    module: WasmModule,
                    host: EmbedderHost, ->
                PthreadCreateJsWasmNode(
                    language = language,
                    module = module,
                    host = host,
                    posixThreadRef = pthreadRef,
                )
            }
            EmscriptenHostFunction.EXIT -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.EMSCRIPTEN_THREAD_CLEANUP -> notImplementedFunctionNodeFactory(this)
            EmscriptenHostFunction.EMSCRIPTEN_NOTIFY_MAILBOX_POSTMESSAGE -> notImplementedFunctionNodeFactory(this)
        }

    fun setupModule(
        graalContext: Context,
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

    fun setupChildThreadModule(
        mainThreadEnv: GraalvmSqliteRuntimeInternal,
        childGraalContext: Context,
    ): WasmInstance = childGraalContext.withWasmContext { wasmContext ->
        val envModule = WasmModule.create(moduleName, null)
        val sharedMemory = mainThreadEnv.envModuleInstance.memory(0)
        setupChildMemory(envModule, sharedMemory)
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
        val minSize = Pages(minMemorySize / WASM_MEMORY_PAGE_SIZE)
        val maxSize: Pages
        val is64Bit: Boolean
        if (context.contextOptions.supportMemory64()) {
            maxSize = WASM_MEMORY_64_MAX_PAGES
            is64Bit = true
        } else {
            maxSize = WASM_MEMORY_SQLITE_MAX_PAGES
            is64Bit = false
        }

        envModule.symbolTable().apply {
            val memoryIndex = memoryCount()
            allocateMemory(memoryIndex, minSize.count, maxSize.count, is64Bit, sharedMemory, false, useUnsafeMemory)
            exportMemory(memoryIndex, "memory")
        }
        envModule.addLinkAction { _: WasmContext, instance: WasmInstance ->
            instance.memory(0).apply {
                waitCallback = WasmMemoryWaitCallback(memoryWaiters, host.rootLogger)
                notifyCallback = WasmMemoryNotifyCallback(memoryWaiters, host.rootLogger)
            }
        }
    }

    private fun setupChildMemory(
        envModule: WasmModule,
        sharedMemory: WasmMemory,
    ) {
        envModule.symbolTable().apply {
            allocateExternalMemory(0, sharedMemory, false)
            exportMemory(0, "memory")
        }
        envModule.addLinkAction { _: WasmContext, instance: WasmInstance ->
            instance.memory(0).apply {
                waitCallback = WasmMemoryWaitCallback(memoryWaiters, host.rootLogger)
                notifyCallback = WasmMemoryNotifyCallback(memoryWaiters, host.rootLogger)
            }
        }
    }
}

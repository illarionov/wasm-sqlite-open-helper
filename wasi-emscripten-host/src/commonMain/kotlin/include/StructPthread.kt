/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.include

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr

/**
 * Internal Pthread struct
 *
 * [pthread_impl.h](https://github.com/emscripten-core/emscripten/blob/3.1.61/system/lib/libc/musl/src/internal/pthread_impl.h)
 */
public data class StructPthread(
    val self: WasmPtr<StructPthread>,
    // uintptr_t *dtv;
    val prev: WasmPtr<StructPthread>,
    val next: WasmPtr<StructPthread>,
    val sysinfo: uintptr_t,
    // canary_pad
    val canary: uintptr_t,
    // Part 2 -- implementation details, non-ABI.
//            int tid;
//        int errno_val;
//        volatile int detach_state;
//        volatile int cancel;
//        volatile unsigned char canceldisable, cancelasync;
//        unsigned char tsd_used:1;
//        unsigned char dlerror_flag:1;
//        unsigned char *map_base;
//        size_t map_size;
//        void *stack;
//        size_t stack_size;
//        size_t guard_size;
//        void *result;
//        struct __ptcb *cancelbuf;
//        void **tsd;
//        struct {
//                volatile void *volatile head;
//                long off;
//                volatile void *volatile pending;
//        } robust_list;
//        int h_errno_val;
//        volatile int timer_id;
//        locale_t locale;
//        volatile int killlock[1];
//        char *dlerror_buf;
//        void *stdio_locks;
    /* Part 3 -- the positions of these fields relative to
     * the end of the structure is external and internal ABI. */
//    #ifdef TLS_ABOVE_TP
//    uintptr_t canary;
//    uintptr_t *dtv;
//    #endif

    //    Emscripten specifics
    // thread_profiler_block * _Atomic profilerBlock;

    /**
     * The TLS base to use the main module TLS data. Secondary modules
     * still require dynamic allocation.
     */
    val tlsBase: WasmPtr<Unit>,

    /**
     * The lowest level of the proxying system. Other threads can enqueue
     * messages on the mailbox and notify this thread to asynchronously
     * process them once it returns to its event loop. When this thread is
     * shut down, the mailbox is closed (see below) to prevent further
     * messages from being enqueued and all the remaining queued messages
     * are dequeued and their shutdown handlers are executed. This allows
     * other threads waiting for their messages to be processed to be
     * notified that their messages will not be processed after all.
     */
    val mailbox: WasmPtr<Unit>,

    /**
     * To ensure that no other thread is concurrently enqueueing a message
     * when this thread shuts down, maintain an atomic refcount. Enqueueing
     * threads atomically increment the count from a nonzero number to
     * acquire the mailbox and decrement the count when they finish. When
     * this thread shuts down it will atomically decrement the count and
     * wait until it reaches 0, at which point the mailbox is considered
     * closed and no further messages will be enqueued.
     * _Atomic int mailbox_refcount;
     */
    val mailboxRefcount: Int,

    /**
     * Whether the thread has executed an `Atomics.waitAsync` on this
     * pthread struct and can be notified of new mailbox messages via
     * `Atomics.notify`. Otherwise, such as when the environment does not
     * implement `Atomics.waitAsync` or when the thread has not had a chance
     * to initialize itself yet, the notification has to fall back to the
     * postMessage path. Once this becomes true, it remains true so we never
     * fall back to postMessage unnecessarily.
     */
    val waitingAsync: Int,

    /**
     * When dynamic linking is enabled, threads use this to facilitate the
     * synchronization of loaded code between threads.
     * See [emscripten_futex_wait.c](https://github.com/emscripten-core/emscripten/blob/3.1.61/system/lib/pthread/emscripten_futex_wait.c).
     */
    val sleeping: Byte,
)

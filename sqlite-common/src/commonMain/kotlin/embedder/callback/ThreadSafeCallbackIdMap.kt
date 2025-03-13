/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.embedder.callback

import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized

internal class ThreadSafeCallbackIdMap<K : SqliteCallbackStore.SqliteCallbackId, V : Any>(
    private val ctor: (Int) -> K,
) : SqliteCallbackStore.SqliteCallbackIdMap<K, V> {
    private val lock = SynchronizedObject()
    private var counter: Int = 1
    private val map: MutableMap<K, V> = mutableMapOf()

    override fun put(value: V): K = synchronized(lock) {
        val newId = allocateId()
        map[newId] = value
        newId
    }

    override operator fun get(key: K): V? = synchronized(lock) { map[key] }

    override fun remove(key: K): V? = synchronized(lock) {
        map.remove(key)
    }

    private fun allocateId(): K {
        val start = counter
        var id = start
        while (map.containsKey(ctor(id))) {
            id = id.nextNonZeroId()
            if (id == start) {
                @Suppress("TooGenericExceptionThrown")
                throw RuntimeException("Can not allocate ID")
            }
        }
        counter = id.nextNonZeroId()
        return ctor(id)
    }

    private fun Int.nextNonZeroId(): Int {
        val nextId = this + 1
        return if (nextId != 0) nextId else 1
    }
}

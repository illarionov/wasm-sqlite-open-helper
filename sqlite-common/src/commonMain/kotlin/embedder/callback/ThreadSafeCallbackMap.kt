/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.callback

import ru.pixnews.wasm.sqlite.open.helper.io.lock.SynchronizedObject
import ru.pixnews.wasm.sqlite.open.helper.io.lock.synchronized

internal class ThreadSafeCallbackMap<K : Any, V : Any> : SqliteCallbackStore.SqliteCallbackMap<K, V> {
    private val lock = SynchronizedObject()
    private val map: MutableMap<K, V> = mutableMapOf()

    override fun get(key: K): V? = synchronized(lock) { map[key] }
    override fun set(key: K, value: V): V? =
        synchronized(lock) { map.put(key, value) }
    override fun remove(key: K): V? = synchronized(lock) { map.remove(key) }
}

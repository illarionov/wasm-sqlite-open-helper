/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback

import ru.pixnews.wasm.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteComparatorCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteDb
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteExecCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteProgressCallback
import ru.pixnews.wasm.sqlite.open.helper.sqlite.common.api.SqliteTraceCallback
import java.util.Collections

internal class Sqlite3CallbackStore {
    val sqlite3Comparators: IdMap<Sqlite3ComparatorId, SqliteComparatorCallback> =
        IdMap(Sqlite3CallbackStore::Sqlite3ComparatorId)
    val sqlite3ExecCallbacks: IdMap<Sqlite3ExecCallbackId, SqliteExecCallback> =
        IdMap(Sqlite3CallbackStore::Sqlite3ExecCallbackId)
    val sqlite3TraceCallbacks: MutableMap<WasmPtr<SqliteDb>, SqliteTraceCallback> =
        Collections.synchronizedMap(mutableMapOf())
    val sqlite3ProgressCallbacks: MutableMap<WasmPtr<SqliteDb>, SqliteProgressCallback> =
        Collections.synchronizedMap(mutableMapOf())

    interface CallbackId {
        val id: Int
    }

    @JvmInline
    value class Sqlite3ExecCallbackId(override val id: Int) : CallbackId

    @JvmInline
    value class Sqlite3ComparatorId(override val id: Int) : CallbackId

    class IdMap<K : CallbackId, V : Any>(
        private val ctor: (Int) -> K,
    ) {
        private val lock = Any()
        private var counter: Int = 1
        private val map: MutableMap<K, V> = mutableMapOf()

        fun put(value: V): K = synchronized(lock) {
            val newId = allocateId()
            map[newId] = value
            newId
        }

        operator fun get(key: K): V? = map[key]

        fun remove(key: K): V? = synchronized(lock) {
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
}

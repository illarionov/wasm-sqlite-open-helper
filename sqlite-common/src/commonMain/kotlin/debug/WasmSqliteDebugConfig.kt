/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.debug

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type.KEY_UNDEFINED
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig

@InternalWasmSqliteHelperApi
public class WasmSqliteDebugConfig(
    private val commonConfig: WasmSqliteCommonConfig,
    private val map: Map<WasmSqliteDebugConfigBlock.Key<*>, WasmSqliteDebugFeature>,
) {
    public fun <E : WasmSqliteDebugFeature> getOrCreateDefault(key: WasmSqliteDebugConfigBlock.Key<E>): E {
        return get(key) ?: createDefault(key)
    }

    private operator fun <E : WasmSqliteDebugFeature> get(key: WasmSqliteDebugConfigBlock.Key<E>): E? {
        @Suppress("UNCHECKED_CAST")
        return map[key] as E?
    }

    private fun <E : WasmSqliteDebugFeature> createDefault(
        key: WasmSqliteDebugConfigBlock.Key<E>,
    ): E = key.create(commonConfig, type = KEY_UNDEFINED)
}

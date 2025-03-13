/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.debug

import at.released.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import at.released.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type.KEY_DEFINED
import at.released.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig

/**
 * Debugging options
 */
@WasmSqliteOpenHelperDsl
public class WasmSqliteDebugConfigBlock @InternalWasmSqliteHelperApi constructor() {
    private val config: MutableMap<Key<*>, WasmSqliteDebugFeature.() -> Unit> = mutableMapOf()

    public fun <F : WasmSqliteDebugFeature, K : Key<F>> set(key: K, block: F.() -> Unit = {}) {
        val old = config[key]
        config[key] = {
            @Suppress("UNCHECKED_CAST")
            block(this as F)
            if (old != null) {
                old(this)
            }
        }
    }

    @InternalWasmSqliteHelperApi
    public fun build(
        commonConfig: WasmSqliteCommonConfig,
    ): WasmSqliteDebugConfig {
        val map: Map<Key<*>, WasmSqliteDebugFeature> = config.mapValues { featureBuilderKeyValue ->
            featureBuilderKeyValue.key.create(commonConfig).apply {
                featureBuilderKeyValue.value(this)
            }
        }
        return WasmSqliteDebugConfig(commonConfig, map)
    }

    public interface Key<F : WasmSqliteDebugFeature> : Factory<F>

    @InternalWasmSqliteHelperApi
    public interface Factory<F : WasmSqliteDebugFeature> {
        public fun create(commonConfig: WasmSqliteCommonConfig, type: Type = KEY_DEFINED): F

        public enum class Type {
            KEY_DEFINED,
            KEY_UNDEFINED,
        }
    }
}

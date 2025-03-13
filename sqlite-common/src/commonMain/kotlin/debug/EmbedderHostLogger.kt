/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.debug

import at.released.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import at.released.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock.Factory.Type
import at.released.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig
import at.released.weh.common.api.Logger

/**
 * Configures a logger for messages from Wasi / Emscripten embedder host
 */
public class EmbedderHostLogger private constructor(
    public var logger: Logger,
) : WasmSqliteDebugFeature {
    @InternalWasmSqliteHelperApi
    override val key: WasmSqliteDebugConfigBlock.Key<*> = Companion
    public var enabled: Boolean = true

    public companion object : WasmSqliteDebugConfigBlock.Key<EmbedderHostLogger> {
        override fun create(commonConfig: WasmSqliteCommonConfig, type: Type): EmbedderHostLogger {
            val logger = commonConfig.logger.withTag("Embedder")
            return EmbedderHostLogger(logger)
        }
    }
}

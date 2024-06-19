/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteWasmEnvironment
import ru.pixnews.wasm.sqlite.open.helper.embedder.WasmSqliteCommonConfig

public object GraalvmSqliteEmbedder : SqliteEmbedder<GraalvmSqliteEmbedderConfig, GraalvmRuntimeInstance> {
    @InternalWasmSqliteHelperApi
    override fun createSqliteWasmEnvironment(
        commonConfig: WasmSqliteCommonConfig,
        embedderConfigBuilder: GraalvmSqliteEmbedderConfig.() -> Unit,
    ): SqliteWasmEnvironment<GraalvmRuntimeInstance> {
        val config = GraalvmSqliteEmbedderConfig(commonConfig.logger).apply(embedderConfigBuilder)
        return GraalvmEmbedderBuilder(
            config.graalvmEngine,
            config.host,
            config.sqlite3Binary,
        ).createEnvironment()
    }
}

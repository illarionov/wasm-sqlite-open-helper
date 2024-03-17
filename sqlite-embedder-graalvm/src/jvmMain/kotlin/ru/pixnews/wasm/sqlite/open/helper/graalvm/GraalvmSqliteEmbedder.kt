/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Engine
import org.graalvm.polyglot.Source
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteCapi
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.bindings.SqliteBindings
import ru.pixnews.wasm.sqlite.open.helper.graalvm.ext.withWasmContext
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.emscripten.EmscriptenEnvModuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.preview1.WasiSnapshotPreview1MobuleBuilder
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.GraalvmSqliteCapiImpl
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.Sqlite3CallbackStore
import ru.pixnews.wasm.sqlite.open.helper.graalvm.sqlite.callback.SqliteCallbacksModuleBuilder
import java.net.URL

public object GraalvmSqliteEmbedder : SqliteEmbedder<GraalvmSqliteEmbedderConfig> {
    override fun createCapi(configBuilder: GraalvmSqliteEmbedderConfig.() -> Unit): SqliteCapi {
        val config = GraalvmSqliteEmbedderConfig().apply(configBuilder)
        return createGraalvmSqliteCapi(
            config.graalvmEngine,
            config.host,
            config.sqlite3WasmBinaryUrl,
        )
    }

    private fun createGraalvmSqliteCapi(
        graalvmEngine: Engine,
        host: SqliteEmbedderHost,
        sqlite3WasmBinaryUrl: URL,
    ): SqliteCapi {
        val callbackStore = Sqlite3CallbackStore()
        val graalContext: Context = Context.newBuilder("wasm")
            .engine(graalvmEngine)
            .allowAllAccess(true)
            .build()
        graalContext.initialize("wasm")

        val sqliteCallbacksModuleBuilder = SqliteCallbacksModuleBuilder(graalContext, host, callbackStore)
        graalContext.withWasmContext {
            EmscriptenEnvModuleBuilder(graalContext, host).setupModule()
            WasiSnapshotPreview1MobuleBuilder(graalContext, host).setupModule()
            sqliteCallbacksModuleBuilder.setupModule()
        }

        val sqliteSource: Source = Source.newBuilder("wasm", sqlite3WasmBinaryUrl).build()
        graalContext.eval(sqliteSource)

        val indirectFunctionIndexes = sqliteCallbacksModuleBuilder.setupIndirectFunctionTable()

        val bindings = SqliteBindings(graalContext)
        return GraalvmSqliteCapiImpl(bindings, callbackStore, indirectFunctionIndexes)
    }
}

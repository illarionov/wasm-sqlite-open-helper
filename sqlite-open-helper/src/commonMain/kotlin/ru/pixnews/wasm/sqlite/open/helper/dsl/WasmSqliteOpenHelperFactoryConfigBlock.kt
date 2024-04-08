/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.dsl

import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import ru.pixnews.wasm.sqlite.open.helper.path.DatabasePathResolver

@WasmSqliteOpenHelperDsl
public class WasmSqliteOpenHelperFactoryConfigBlock<E : SqliteEmbedderConfig>(
    defaultDatabasePathResolver: DatabasePathResolver,
) {
    public var logger: Logger = Logger
    internal var debugConfigBlock: DebugConfigBlock.() -> Unit = { }
        private set
    public var pathResolver: DatabasePathResolver = defaultDatabasePathResolver
    internal var openParams: OpenParamsBlock.() -> Unit = {}
        private set
    internal var embedderConfig: E.() -> Unit = {}
        private set

    public fun embedder(block: E.() -> Unit) {
        val oldConfig = embedderConfig
        embedderConfig = {
            oldConfig()
            block()
        }
    }

    public fun debug(block: DebugConfigBlock.() -> Unit) {
        val old = this.debugConfigBlock
        debugConfigBlock = {
            old()
            block()
        }
    }

    public fun openParams(block: OpenParamsBlock.() -> Unit) {
        val old = this.openParams
        this.openParams = {
            old()
            block()
        }
    }
}

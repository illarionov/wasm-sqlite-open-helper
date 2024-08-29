/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.dsl

import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.debug.WasmSqliteDebugConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

@WasmSqliteOpenHelperDsl
public class WasmSqliteDriverConfigBlock<E : SqliteEmbedderConfig> {
    /**
     * Sets the logger used to log debug messages from SupportSQLiteOpenHelper.
     *
     * By default, messages are not logged
     */
    public var logger: Logger = Logger

    internal var debugConfigBlock: WasmSqliteDebugConfigBlock.() -> Unit = { }
        private set
    internal var openParams: OpenParamsBlock.() -> Unit = {}
        private set
    internal var embedderConfig: E.() -> Unit = {}
        private set

    /**
     * Sets the configuration of the Wasm ebedder
     */
    public fun embedder(block: E.() -> Unit) {
        val oldConfig = embedderConfig
        embedderConfig = {
            oldConfig()
            block()
        }
    }

    /**
     * Sets the debugging options
     */
    public fun debug(block: WasmSqliteDebugConfigBlock.() -> Unit) {
        val old = this.debugConfigBlock
        debugConfigBlock = {
            old()
            block()
        }
    }

    /**
     * Sets the parameters used when opening a database
     */
    public fun openParams(block: OpenParamsBlock.() -> Unit) {
        val old = this.openParams
        this.openParams = {
            old()
            block()
        }
    }
}

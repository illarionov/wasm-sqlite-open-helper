/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:JvmName("AndroidWasmSQLiteDriverBuilder")

package at.released.wasm.sqlite.driver

import android.content.Context
import at.released.cassettes.playhead.AndroidAssetsAssetManager
import at.released.wasm.sqlite.driver.dsl.WasmSqliteDriverConfigBlock
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedder
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.wasm.sqlite.open.helper.embedder.SqliteRuntime

/**
 * Creates a [SQLiteDriver] with the specified [block] configuration.
 *
 * @param context Application context
 * @param embedder WebAssembly Runtime (embedder) for running SQLite compiled to Wasm.
 * For example, GraalvmSqliteEmbedder
 */
public fun <E : SqliteEmbedderConfig, R : SqliteRuntime> WasmSQLiteDriver(
    embedder: SqliteEmbedder<E, R>,
    context: Context,
    block: WasmSqliteDriverConfigBlock<E>.() -> Unit = {},
): WasmSQLiteDriver<R> = WasmSQLiteDriver(
    embedder = embedder,
    defaultWasmSourceReader = AndroidAssetsAssetManager(context.assets),
    block = block,
)

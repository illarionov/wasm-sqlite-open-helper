/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chasm

import at.released.weh.common.api.Logger
import at.released.weh.host.EmbedderHost
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

/**
 * Configuration of the Chasm engine
 */
@WasmSqliteOpenHelperDsl
public class ChasmSqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
    defaultWasmSourceReader: WasmSourceReader,
) : SqliteEmbedderConfig {
    /**
     * Used Sqlite WebAssembly binary file
     */
    public var sqlite3Binary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcu346

    /**
     * Sets the Wasm source reader responsible for reading a WebAssembly binary.
     * This can be overridden to read Wasm files from non-standard locations.
     * See [JvmResourcesWasmBinaryReader][ru.pixnews.wasm.sqlite.binary.reader.JvmResourcesWasmBinaryReader]
     * or [LinuxWasmSourceReader][ru.pixnews.wasm.sqlite.binary.reader.LinuxWasmSourceReader] for examples of custom
     * implementations.
     */
    public var wasmSourceReader: WasmSourceReader = defaultWasmSourceReader

    /**
     * Implementation of a host object that provides access from the WebAssembly to external host resources.
     */
    public var host: EmbedderHost = EmbedderHost.Builder().apply {
        this.rootLogger = rootLogger
        this.directories()
            .setAllowRootAccess(true)
            .setCurrentWorkingDirectory(".")
    }.build()
}

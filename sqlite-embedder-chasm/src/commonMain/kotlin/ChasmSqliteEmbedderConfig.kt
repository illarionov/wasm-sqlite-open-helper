/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chasm

import at.released.cassettes.playhead.AssetManager
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.weh.common.api.Logger
import at.released.weh.host.EmbedderHost
import io.github.charlietap.chasm.config.RuntimeConfig

/**
 * Configuration of the Chasm engine
 */
@WasmSqliteOpenHelperDsl
public class ChasmSqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
    defaultWasmSourceReader: AssetManager,
) : SqliteEmbedderConfig {
    private var _sqlite3Binary: WasmSqliteConfiguration? = null

    /**
     * Used Sqlite WebAssembly binary file
     */
    public var sqlite3Binary: WasmSqliteConfiguration
        get() = _sqlite3Binary ?: error("ChasmSqliteEmbedderConfig.sqlite3Binary must be set")
        set(value) {
            _sqlite3Binary = value
        }

    /**
     * Sets the Wasm source reader responsible for reading a WebAssembly binary.
     * This can be overridden to read Wasm files from non-standard locations.
     * See [JvmResourcesWasmBinaryReader][at.released.wasm.sqlite.binary.reader.JvmResourcesWasmBinaryReader]
     * or [LinuxWasmSourceReader][at.released.wasm.sqlite.binary.reader.LinuxWasmSourceReader] for examples of custom
     * implementations.
     */
    public var wasmSourceReader: AssetManager = defaultWasmSourceReader

    /**
     * Sets Chasm Runtime Config
     */
    public var runtimeConfig: RuntimeConfig = RuntimeConfig(bytecodeFusion = false)

    /**
     * Implementation of a host object that provides access from the WebAssembly to external host resources.
     */
    public var host: EmbedderHost = EmbedderHost {
        logger = rootLogger
        fileSystem {
            unrestricted = true
        }
    }
}

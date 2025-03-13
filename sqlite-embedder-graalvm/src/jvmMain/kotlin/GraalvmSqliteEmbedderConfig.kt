/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.graalvm

import at.released.cassettes.playhead.AssetManager
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.weh.common.api.Logger
import at.released.weh.host.EmbedderHost
import org.graalvm.polyglot.Engine

/**
 * Configuration of the GraalVM engine
 */
@WasmSqliteOpenHelperDsl
public class GraalvmSqliteEmbedderConfig internal constructor(
    embedderRootLogger: Logger,
    defaultWasmSourceReader: AssetManager,
) : SqliteEmbedderConfig {
    /**
     * Instance of the GraalVM WebAssembly engine. Single instance of the Engine can be reused to speed
     * up initialization.
     *
     * Should be `Engine.create("wasm")`.
     *
     * See: [GraalVM Managing the code cache](https://www.graalvm.org/latest/reference-manual/embed-languages/#managing-the-code-cache)
     */
    public var graalvmEngine: Engine = Engine.create("wasm")

    private var _sqlite3Binary: WasmSqliteConfiguration? = null

    /**
     * Sets used Sqlite WebAssembly binary file
     */
    public var sqlite3Binary: WasmSqliteConfiguration
        get() = _sqlite3Binary ?: error("GraalvmSqliteEmbedderConfig.sqlite3Binary must be set")
        set(value) {
            _sqlite3Binary = value
        }

    /**
     * Sets the Wasm source reader responsible for reading a WebAssembly binary.
     * This can be overridden to read Wasm files from non-standard locations.
     * See [JvmResourcesWasmBinaryReader][at.released.wasm.sqlite.binary.reader.JvmResourcesWasmBinaryReader] for
     * example of custom implementations.
     */
    public var wasmSourceReader: AssetManager = defaultWasmSourceReader

    /**
     * Implementation of a host object that provides access from the WebAssembly to external host resources.
     */
    public var host: EmbedderHost = EmbedderHost {
        logger = embedderRootLogger
        fileSystem {
            unrestricted = true
        }
    }
}

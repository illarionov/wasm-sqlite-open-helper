/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory

import at.released.weh.common.api.Logger
import at.released.weh.host.EmbedderHost
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Machine
import ru.pixnews.wasm.sqlite.binary.SqliteAndroidWasmEmscriptenIcu346
import ru.pixnews.wasm.sqlite.binary.base.WasmSqliteConfiguration
import ru.pixnews.wasm.sqlite.binary.reader.WasmSourceReader
import ru.pixnews.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig

/**
 * Configuration of the Chicory engine
 */
@WasmSqliteOpenHelperDsl
public class ChicorySqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
    defaultWasmSourceReader: WasmSourceReader,
) : SqliteEmbedderConfig {
    /**
     * Sets used Sqlite WebAssembly binary file
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
     * Overrides Chicory machine factory. It can be used to install the Experimental Wasm-to-JVM bytecode AOT engine.
     *
     * See: [https://github.com/dylibso/chicory/tree/main/aot](https://github.com/dylibso/chicory/tree/main/aot)
     */
    public var machineFactory: ((Instance) -> Machine)? = null

    /**
     * Implementation of a host object that provides access from the WebAssembly to external host resources.
     */
    public var host: EmbedderHost = EmbedderHost.Builder().apply { this.rootLogger = rootLogger }.build()
}

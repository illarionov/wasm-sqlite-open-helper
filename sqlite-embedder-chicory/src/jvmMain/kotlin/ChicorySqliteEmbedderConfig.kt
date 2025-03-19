/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.chicory

import at.released.cassettes.playhead.AssetManager
import at.released.wasm.sqlite.binary.aot.SqliteAndroidWasmEmscriptenIcuAot349
import at.released.wasm.sqlite.binary.base.WasmSqliteConfiguration
import at.released.wasm.sqlite.open.helper.WasmSqliteOpenHelperDsl
import at.released.wasm.sqlite.open.helper.embedder.SqliteEmbedderConfig
import at.released.weh.common.api.Logger
import at.released.weh.host.EmbedderHost
import com.dylibso.chicory.runtime.Instance
import com.dylibso.chicory.runtime.Machine
import com.dylibso.chicory.runtime.Memory
import com.dylibso.chicory.wasm.types.MemoryLimits

/**
 * Configuration of the Chicory engine
 */
@WasmSqliteOpenHelperDsl
public class ChicorySqliteEmbedderConfig internal constructor(
    rootLogger: Logger,
    defaultWasmSourceReader: AssetManager,
) : SqliteEmbedderConfig {
    /**
     * Sets used Sqlite WebAssembly binary file
     */
    public var sqlite3Binary: WasmSqliteConfiguration = SqliteAndroidWasmEmscriptenIcuAot349

    /**
     * Sets the Wasm source reader responsible for reading a WebAssembly binary.
     * This can be overridden to read Wasm files from non-standard locations.
     * See [JvmResourcesAssetManager][at.released.cassettes.playhead.JvmResourcesAssetManager]
     * or [LinuxAssetManager][at.released.cassettes.playhead.LinuxAssetManager] for examples of custom
     * implementations.
     */
    public var wasmSourceReader: AssetManager = defaultWasmSourceReader

    /**
     * Overrides Chicory machine factory. It can be used to install the Experimental Wasm-to-JVM bytecode AOT engine.
     *
     * See: [https://github.com/dylibso/chicory/tree/main/aot](https://github.com/dylibso/chicory/tree/main/aot)
     */
    public var machineFactory: ((Instance) -> Machine)? = null

    /**
     * Overrides Chicory memory factory.
     *
     * By default it will be used [ByteBufferMemory][com.dylibso.chicory.runtime.ByteBufferMemory]
     * with [ExactMemAllocStrategy][com.dylibso.chicory.runtime.alloc.ExactMemAllocStrategy].
     *
     * See: [https://chicory.dev/docs/advanced/memory](https://chicory.dev/docs/advanced/memory)
     */
    public var memoryFactory: ((MemoryLimits) -> Memory)? = null

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

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import at.released.weh.bindings.graalvm241.GraalvmEmscriptenEnvironment
import at.released.weh.host.base.memory.Memory
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.Value
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteEmbedderRuntimeInfo
import ru.pixnews.wasm.sqlite.open.helper.embedder.SqliteRuntimeInternal
import ru.pixnews.wasm.sqlite.open.helper.embedder.callback.SqliteCallbackStore
import ru.pixnews.wasm.sqlite.open.helper.embedder.exports.SqliteExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.exports.GraalvmSqliteExports
import ru.pixnews.wasm.sqlite.open.helper.graalvm.host.module.sqlitecb.GraalvmSqliteCallbackFunctionIndexes
import java.util.concurrent.ThreadFactory

internal class GraalvmSqliteRuntimeInternal internal constructor(
    private val mainModuleName: String,
    private val rootContext: Context,
    private val embedderInfo: SqliteEmbedderRuntimeInfo,
    internal val emscriptenEnvironment: GraalvmEmscriptenEnvironment,
    override val callbackStore: SqliteCallbackStore,
    override val callbackFunctionIndexes: GraalvmSqliteCallbackFunctionIndexes,
) : SqliteRuntimeInternal<GraalvmRuntime> {
    private val mainBindings: Value
        get() = requireNotNull(rootContext.getBindings("wasm").getMember(mainModuleName)) {
            "module $mainModuleName not loaded"
        }

    override val sqliteExports: SqliteExports = GraalvmSqliteExports(::mainBindings)
    override val memory: Memory = emscriptenEnvironment.memory
    override val runtimeInstance: GraalvmRuntime = object : GraalvmRuntime {
        override val embedderInfo: SqliteEmbedderRuntimeInfo = this@GraalvmSqliteRuntimeInternal.embedderInfo
        override val managedThreadFactory: ThreadFactory
            get() = emscriptenEnvironment.managedThreadFactory
    }

    override fun close() {
        emscriptenEnvironment.close()
    }
}

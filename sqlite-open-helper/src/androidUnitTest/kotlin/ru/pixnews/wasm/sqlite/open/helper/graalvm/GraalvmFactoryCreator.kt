/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm

import org.graalvm.polyglot.Engine
import ru.pixnews.wasm.sqlite.open.helper.SqliteAndroidWasmEmscriptenIcuMtPthread345
import ru.pixnews.wasm.sqlite.open.helper.base.TestOpenHelperFactoryCreator

internal class GraalvmFactoryCreator(
    initialGraalvmEngine: Engine? = WASM_GRAALVM_ENGINE,
) : TestOpenHelperFactoryCreator<GraalvmSqliteEmbedderConfig>(
    embedder = GraalvmSqliteEmbedder,
    defaultSqliteBinary = SqliteAndroidWasmEmscriptenIcuMtPthread345,
    defaultEmbedderConfig = { sqlite3Binary ->
        this.sqlite3Binary = sqlite3Binary
        if (initialGraalvmEngine != null) {
            this.graalvmEngine = initialGraalvmEngine
        }
    },
) {
    internal companion object {
        internal val WASM_GRAALVM_ENGINE: Engine = Engine.create("wasm")
    }
}

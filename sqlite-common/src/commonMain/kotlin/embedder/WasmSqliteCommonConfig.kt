/*
 * Copyright 2024-2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder

import at.released.cassettes.playhead.AssetManager
import at.released.weh.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public interface WasmSqliteCommonConfig {
    public val logger: Logger
    public val wasmReader: AssetManager
}

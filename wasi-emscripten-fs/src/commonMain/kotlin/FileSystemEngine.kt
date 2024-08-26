/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl.FileSystemCommonConfig
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl.FileSystemEngineConfig

public interface FileSystemEngine<E : FileSystemEngineConfig> {
    @InternalWasmSqliteHelperApi
    public fun create(
        commonConfig: FileSystemCommonConfig,
        engineConfig: E.() -> Unit,
    ): FileSystem
}

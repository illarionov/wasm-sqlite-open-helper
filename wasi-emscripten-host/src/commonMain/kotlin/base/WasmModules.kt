/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base

public object WasmModules {
    public const val ENV_MODULE_NAME: String = "env"
    public const val SQLITE3_CALLBACK_MANAGER_MODULE_NAME: String = "sqlite3-callback-manager"
    public const val WASI_SNAPSHOT_PREVIEW1_MODULE_NAME: String = "wasi_snapshot_preview1"
}

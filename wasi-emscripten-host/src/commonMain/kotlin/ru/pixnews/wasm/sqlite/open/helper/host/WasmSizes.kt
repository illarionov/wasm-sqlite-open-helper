/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host

public object WasmSizes {
    public const val WASM_MEMORY_SQLITE_MAX_PAGES: Long = 32_768L
    public const val WASM_MEMORY_PAGE_SIZE: Long = 65_536L
    public const val WASM_MEMORY_32_MAX_PAGES: Long = 65_536L
    public const val WASM_MEMORY_64_MAX_PAGES: Long = 281_474_976_710_656L
}

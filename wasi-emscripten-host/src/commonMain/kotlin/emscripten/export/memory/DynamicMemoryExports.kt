/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.export.memory

import ru.pixnews.wasm.sqlite.open.helper.host.base.binding.WasmFunctionBinding

public interface DynamicMemoryExports {
    /**
     * POSIX malloc()
     */
    public val malloc: WasmFunctionBinding?

    /**
     * POSIX free()
     */
    public val free: WasmFunctionBinding?
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory.ext

import com.dylibso.chicory.wasm.types.ValueType
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType

internal val WasmValueType.chicory: ValueType
    get() = ValueType.byId(
        requireNotNull(opcode) {
            "Can not convert Wasi type without opcode"
        }.toLong(),
    )
// New version:
// get() = ValueType.forId(requireNotNull(opcode) { "Can not convert Wasi type without opcode" }.toInt())

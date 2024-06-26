/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base

import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmValueType.WebAssemblyTypes.I32
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.WasiTypename

public val POINTER: WasmValueType get() = I32

public val WasmValueType.pointer: WasmValueType
    get() = POINTER

public val WasiTypename.pointer: WasmValueType
    get() = POINTER

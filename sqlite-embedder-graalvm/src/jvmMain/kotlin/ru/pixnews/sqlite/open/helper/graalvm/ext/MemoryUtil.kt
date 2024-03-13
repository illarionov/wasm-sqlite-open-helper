/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.ext

import org.graalvm.polyglot.Value
import ru.pixnews.sqlite.open.helper.common.api.WasmPtr
import ru.pixnews.sqlite.open.helper.host.memory.Memory
import ru.pixnews.sqlite.open.helper.host.memory.readZeroTerminatedString

// TODO: remove
internal fun <P : Any?> Value.asWasmAddr(): WasmPtr<P> = WasmPtr(asInt())

// TODO: remove
internal fun <P : Any?> Array<Any>.asWasmPtr(idx: Int): WasmPtr<P> = WasmPtr(this[idx] as Int)

internal fun Memory.readNullTerminatedString(offsetValue: Value): String? {
    return if (!offsetValue.isNull) {
        this.readZeroTerminatedString(offsetValue.asWasmAddr())
    } else {
        null
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.host.functiontable

public sealed interface IndirectFunctionRef {
    public val ref: Int

    @JvmInline
    public value class FuncRef(
        override val ref: Int,
    ) : IndirectFunctionRef

    @JvmInline
    public value class ExternalRef(
        override val ref: Int,
    ) : IndirectFunctionRef
}

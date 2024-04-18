/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.embedder.functiontable

public interface Sqlite3CallbackFunctionIndexes {
    public val execCallbackFunction: IndirectFunctionTableIndex
    public val traceFunction: IndirectFunctionTableIndex
    public val progressFunction: IndirectFunctionTableIndex
    public val comparatorFunction: IndirectFunctionTableIndex
    public val destroyComparatorFunction: IndirectFunctionTableIndex
    public val loggingCallbackFunction: IndirectFunctionTableIndex
}

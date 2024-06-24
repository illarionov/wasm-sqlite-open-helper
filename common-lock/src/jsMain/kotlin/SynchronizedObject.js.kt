/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.io.lock

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@InternalWasmSqliteHelperApi
public actual typealias SynchronizedObject = Any

@InternalWasmSqliteHelperApi
public actual inline fun <T> synchronized(lock: SynchronizedObject, block: () -> T): T =
    kotlinx.atomicfu.locks.synchronized(lock, block)

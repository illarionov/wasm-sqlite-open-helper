/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.io.lock

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import kotlinx.atomicfu.locks.withLock as atomifuWithLock

@InternalWasmSqliteHelperApi
public actual typealias ReentrantLock = kotlinx.atomicfu.locks.ReentrantLock

@InternalWasmSqliteHelperApi
public actual fun reentrantLock(): ReentrantLock = kotlinx.atomicfu.locks.reentrantLock()

@InternalWasmSqliteHelperApi
public actual inline fun <T> ReentrantLock.withLock(block: () -> T): T = atomifuWithLock(block)

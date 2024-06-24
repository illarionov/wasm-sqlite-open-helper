/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.io.lock

import kotlinx.atomicfu.locks.SynchronizedObject
import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi
import kotlinx.atomicfu.locks.withLock as atomicfuWithLock

@InternalWasmSqliteHelperApi
public actual typealias ReentrantLock = SynchronizedObject

@InternalWasmSqliteHelperApi
public actual fun reentrantLock(): ReentrantLock = kotlinx.atomicfu.locks.reentrantLock()

@InternalWasmSqliteHelperApi
public actual inline fun <T> ReentrantLock.withLock(block: () -> T): T = atomicfuWithLock(block)

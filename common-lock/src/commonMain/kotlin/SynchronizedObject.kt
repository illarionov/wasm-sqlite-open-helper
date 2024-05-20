/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.io.lock

import ru.pixnews.wasm.sqlite.open.helper.common.api.InternalWasmSqliteHelperApi

@Suppress("EMPTY_PRIMARY_CONSTRUCTOR")
@InternalWasmSqliteHelperApi
public expect open class SynchronizedObject()

@InternalWasmSqliteHelperApi
public expect inline fun <T> synchronized(lock: SynchronizedObject, block: () -> T): T

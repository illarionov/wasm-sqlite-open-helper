/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "UnusedPrivateProperty")

package ru.pixnews.wasm.sqlite.open.helper.exception

public expect open class AndroidSqlException : RuntimeException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
}

public expect open class AndroidSqliteException : AndroidSqlException {
    public constructor()
    public constructor(message: String?)
    public constructor(message: String?, cause: Throwable?)
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("IDENTIFIER_LENGTH")

package ru.pixnews.wasm.sqlite.test.utils

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import kotlin.test.fail

public open class TestLogger : Logger {
    override fun withTag(tag: String): Logger = this
    override fun v(throwable: Throwable?, message: () -> String): Unit = fail("Should not be called")
    override fun d(throwable: Throwable?, message: () -> String): Unit = fail("Should not be called")
    override fun i(throwable: Throwable?, message: () -> String): Unit = fail("Should not be called")
    override fun w(throwable: Throwable?, message: () -> String): Unit = fail("Should not be called")
    override fun e(throwable: Throwable?, message: () -> String): Unit = fail("Should not be called")
    override fun a(throwable: Throwable?, message: () -> String): Unit = fail("Should not be called")
}

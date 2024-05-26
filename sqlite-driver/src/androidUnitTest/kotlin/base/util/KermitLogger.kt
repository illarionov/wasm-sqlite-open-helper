/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.driver.base.util

import co.touchlab.kermit.Severity
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import co.touchlab.kermit.Logger as KermitLogger

@Suppress("IDENTIFIER_LENGTH")
public class KermitLogger(
    tag: String = "WSOH",
    private val minSeverity: Severity = Severity.Verbose,
) : Logger {
    private val delegate: KermitLogger = KermitLogger.apply { setMinSeverity(minSeverity) }.withTag(tag)

    override fun withTag(tag: String): Logger = KermitLogger(tag, minSeverity)

    override fun v(throwable: Throwable?, message: () -> String) {
        delegate.v(message(), throwable)
    }

    override fun d(throwable: Throwable?, message: () -> String) {
        delegate.d(message(), throwable)
    }

    override fun i(throwable: Throwable?, message: () -> String) {
        delegate.i(message(), throwable)
    }

    override fun w(throwable: Throwable?, message: () -> String) {
        delegate.w(message(), throwable)
    }

    override fun e(throwable: Throwable?, message: () -> String) {
        delegate.e(message(), throwable)
    }

    override fun a(throwable: Throwable?, message: () -> String) {
        delegate.a(message(), throwable)
    }
}

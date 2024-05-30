/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("WRONG_OVERLOADING_FUNCTION_ARGUMENTS")

package ru.pixnews.wasm.sqlite.open.helper.chicory.host

import com.dylibso.chicory.log.Logger.Level
import com.dylibso.chicory.log.Logger.Level.ALL
import com.dylibso.chicory.log.Logger.Level.DEBUG
import com.dylibso.chicory.log.Logger.Level.ERROR
import com.dylibso.chicory.log.Logger.Level.INFO
import com.dylibso.chicory.log.Logger.Level.OFF
import com.dylibso.chicory.log.Logger.Level.TRACE
import com.dylibso.chicory.log.Logger.Level.WARNING
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import java.util.function.Supplier
import com.dylibso.chicory.log.Logger as ChicoryLogger

internal class ChicoryLogger(
    logger: Logger,
    private val severity: Level = INFO,
) : ChicoryLogger {
    private val logger = logger.withTag("Chicory")

    override fun log(level: Level, msg: String?, throwable: Throwable?) {
        when (level) {
            ALL -> logger.v(throwable, msg::toString)
            TRACE -> logger.v(throwable, msg::toString)
            DEBUG -> logger.d(throwable, msg::toString)
            INFO -> logger.i(throwable, msg::toString)
            WARNING -> logger.w(throwable, msg::toString)
            ERROR -> logger.e(throwable, msg::toString)
            OFF -> logger.a(throwable, msg::toString)
        }
    }

    override fun isLoggable(level: Level): Boolean = level.severity >= severity.severity

    override fun trace(msgSupplier: Supplier<String>) {
        if (isLoggable(TRACE)) {
            logger.v(message = msgSupplier::get)
        }
    }

    override fun trace(msgSupplier: Supplier<String>, throwable: Throwable?) {
        if (isLoggable(TRACE)) {
            logger.v(throwable, msgSupplier::get)
        }
    }

    override fun debug(msgSupplier: Supplier<String>) {
        if (isLoggable(DEBUG)) {
            logger.d(message = msgSupplier::get)
        }
    }

    override fun debug(msgSupplier: Supplier<String>, throwable: Throwable?) {
        if (isLoggable(DEBUG)) {
            logger.d(throwable, msgSupplier::get)
        }
    }

    override fun info(msgSupplier: Supplier<String>) {
        if (isLoggable(INFO)) {
            logger.i(message = msgSupplier::get)
        }
    }

    override fun info(msgSupplier: Supplier<String>, throwable: Throwable?) {
        if (isLoggable(INFO)) {
            logger.i(throwable, msgSupplier::get)
        }
    }

    override fun warn(msgSupplier: Supplier<String>) {
        if (isLoggable(WARNING)) {
            logger.w(message = msgSupplier::get)
        }
    }

    override fun warn(msgSupplier: Supplier<String>, throwable: Throwable?) {
        if (isLoggable(WARNING)) {
            logger.w(throwable, msgSupplier::get)
        }
    }

    override fun error(msgSupplier: Supplier<String>) {
        if (isLoggable(ERROR)) {
            logger.e(message = msgSupplier::get)
        }
    }

    override fun error(msgSupplier: Supplier<String>, throwable: Throwable?) {
        if (isLoggable(ERROR)) {
            logger.e(throwable, msgSupplier::get)
        }
    }
}

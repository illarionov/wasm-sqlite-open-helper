/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.platform.synchronized

internal class OperationLog(
    private val debugConfig: SQLiteDebug,
    rootLogger: Logger,
    private val currentTimeMillisProvider: () -> Long,
    private val timestampFormatter: (Long) -> String,
) {
    private val logger = rootLogger.withTag("OperationLog")
    private val operations: Array<Operation?> = arrayOfNulls(MAX_RECENT_OPERATIONS)
    private var index: Int = 0
    private var generation: Int = 0

    fun beginOperation(
        kind: String,
        sql: String?,
        bindArgs: List<Any?> = emptyList(),
    ): Int = synchronized(operations) {
        val index = (index + 1) % MAX_RECENT_OPERATIONS
        var operation = operations[index]
        if (operation == null) {
            operation = Operation()
            operations[index] = operation
        } else {
            operation.finished = false
            operation.exception = null
        }
        operation.startTime = currentTimeMillisProvider()
        operation.kind = kind
        operation.sql = sql
        operation.bindArgs = bindArgs.map {
            if (it is ByteArray) {
                // Don't hold onto the real byte array longer than necessary.
                arrayOf<Byte>()
            } else {
                it
            }
        }
        operation.cookie = newOperationCookieLocked(index)
        this.index = index
        return operation.cookie
    }

    fun failOperation(cookie: Int, ex: Exception?) = synchronized(operations) {
        val operation = getOperationLocked(cookie)
        if (operation != null) {
            operation.exception = ex
        }
    }

    fun endOperation(cookie: Int) = synchronized(operations) {
        if (endOperationDeferLogLocked(cookie)) {
            logOperationLocked(cookie, null)
        }
    }

    fun endOperationDeferLog(cookie: Int): Boolean = synchronized(operations) {
        return endOperationDeferLogLocked(cookie)
    }

    fun logOperation(cookie: Int, detail: String?) = synchronized(operations) {
        logOperationLocked(cookie, detail)
    }

    private fun endOperationDeferLogLocked(cookie: Int): Boolean {
        val operation = getOperationLocked(cookie)
        if (operation != null) {
            operation.endTime = currentTimeMillisProvider()
            operation.finished = true
            return debugConfig.sqlLog && debugConfig.shouldLogSlowQuery(operation.endTime - operation.startTime)
        }
        return false
    }

    private fun logOperationLocked(cookie: Int, detail: String?) {
        val operation = getOperationLocked(cookie) ?: return
        val msg = StringBuilder()
        operation.describe(msg, false)
        if (detail != null) {
            msg.append(", ").append(detail)
        }
        logger.d(message = msg::toString)
    }

    private fun newOperationCookieLocked(index: Int): Int {
        val generation = generation++
        return generation shl COOKIE_GENERATION_SHIFT or index
    }

    private fun getOperationLocked(cookie: Int): Operation? {
        val index = cookie and COOKIE_INDEX_MASK
        val operation = operations[index]
        return if (operation!!.cookie == cookie) operation else null
    }

    fun describeCurrentOperation(): String? = synchronized(operations) {
        val operation = operations[index]
        if (operation != null && !operation.finished) {
            val msg = StringBuilder()
            operation.describe(msg, false)
            return msg.toString()
        }
        return null
    }

    fun dump(verbose: Boolean): String = synchronized(operations) {
        buildString {
            appendLine("  Most recently executed operations:")
            var index = index
            var operation: Operation? = operations[index]
            if (operation != null) {
                var operationNo = 0
                do {
                    append("    ")
                    append(operationNo)
                    append(": [")
                    append(timestampFormatter(operation!!.startTime))
                    append("] ")
                    operation.describe(this, verbose)
                    appendLine()

                    if (index > 0) {
                        index -= 1
                    } else {
                        index = MAX_RECENT_OPERATIONS - 1
                    }
                    operationNo += 1
                    operation = operations[index]
                } while (operation != null && operationNo < MAX_RECENT_OPERATIONS)
            } else {
                appendLine("    <none>")
            }
        }
    }

    private inner class Operation {
        var startTime: Long = 0
        var endTime: Long = 0
        var kind: String? = null
        var sql: String? = null
        var bindArgs: List<Any?>? = null
        var finished: Boolean = false
        var exception: Exception? = null
        var cookie: Int = 0

        private val status: String
            get() = when {
                !finished -> "running"
                exception != null -> "failed"
                else -> "succeeded"
            }

        fun describe(msg: StringBuilder, verbose: Boolean) = with(msg) {
            append(kind)
            if (finished) {
                append(" took ").append(endTime - startTime).append("ms")
            } else {
                append(" started ").append(currentTimeMillisProvider() - startTime).append("ms ago")
            }
            append(" - ").append(status)
            if (sql != null) {
                append(", sql=\"").append(trimSqlForDisplay(sql)).append("\"")
            }
            if (verbose) {
                bindArgs?.let { appendBindArgs(it) }
            }
            if (exception != null) {
                append(", exception=\"").append(exception!!.message).append("\"")
            }
        }

        private fun StringBuilder.appendBindArgs(args: List<Any?>) {
            if (args.isEmpty()) {
                return
            }
            args.joinTo(this, ", ", prefix = ", bindArgs=[", postfix = "]") { arg ->
                when (arg) {
                    null -> "null"
                    is ByteArray -> "<byte[]>"
                    is String -> "\"$arg\""
                    else -> arg.toString()
                }
            }
        }
    }

    companion object {
        private const val MAX_RECENT_OPERATIONS = 20
        private const val COOKIE_GENERATION_SHIFT = 8
        private const val COOKIE_INDEX_MASK = 0xff
        private val TRIM_SQL_PATTERN: Regex = """\s*\n+\s*""".toRegex()

        internal fun trimSqlForDisplay(sql: String?): String {
            return TRIM_SQL_PATTERN.replace(sql ?: "", " ")
        }
    }
}

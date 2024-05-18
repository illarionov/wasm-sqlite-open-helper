/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal.connection

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.internal.SQLiteDebug
import ru.pixnews.wasm.sqlite.open.helper.internal.platform.synchronized

internal class OperationLog(
    private val debugConfig: SQLiteDebug,
    rootLogger: Logger,
    private val currentTimeProvider: () -> Long,
    private val uptimeProvider: () -> Long,
    private val pathProvider: () -> String,
    private val timestampFormatter: (Long) -> String,
    private val onStatementExecuted: (elapsedTime: Long) -> Unit,
) {
    private val logger = rootLogger.withTag("OperationLog")
    private val operations: Array<Operation?> = arrayOfNulls(MAX_RECENT_OPERATIONS)
    private var index: Int = 0
    private var generation: Int = 0
    private var resultLong: Long = Long.MIN_VALUE
    private var resultString: String? = null

    fun beginOperation(
        kind: String,
        sql: String?,
        bindArgs: List<Any?> = emptyList(),
    ): Int {
        resultLong = Long.MIN_VALUE
        resultString = null

        synchronized(operations) {
            val index = (index + 1) % MAX_RECENT_OPERATIONS
            var operation = operations[index]
            if (operation == null) {
                operation = Operation()
                operations[index] = operation
            } else {
                operation.finished = false
                operation.exception = null
            }
            operation.startWallTime = currentTimeProvider()
            operation.startTime = uptimeProvider()
            operation.kind = kind
            operation.sql = sql
            operation.path = pathProvider()
            operation.resultLong = Long.MIN_VALUE
            operation.resultString = null
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
    }

    fun setResult(longResult: Long) {
        resultLong = longResult
    }

    fun setResult(stringResult: String?) {
        resultString = stringResult
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
            operation.endTime = uptimeProvider()
            operation.finished = true
            val execTime = operation.endTime - operation.startTime
            onStatementExecuted(execTime)
            return debugConfig.sqlLog && debugConfig.shouldLogSlowQuery(execTime)
        }
        return false
    }

    @Suppress("COMPACT_OBJECT_INITIALIZATION")
    private fun logOperationLocked(cookie: Int, detail: String?) {
        val operation = getOperationLocked(cookie) ?: return
        operation.resultString = resultString
        operation.resultLong = resultLong
        logger.d {
            buildString {
                operation.describe(this, true)
                if (detail != null) {
                    append(", ").append(detail)
                }
            }
        }
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

    internal inline fun <R : Any?> useOperation(
        kind: String,
        sql: String?,
        bindArgs: List<Any?> = emptyList(),
        block: (cookie: Int) -> R,
    ): R {
        val cookie = beginOperation(kind, sql, bindArgs)
        try {
            return block(cookie)
        } catch (@Suppress("TooGenericExceptionCaught") ex: RuntimeException) {
            failOperation(cookie, ex)
            throw ex
        } finally {
            endOperation(cookie)
        }
    }

    fun dump(): String = synchronized(operations) {
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
                    operation.describe(this, false) // Never dump bingargs in a bugreport
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
        var startWallTime: Long = 0 // in System.currentTimeMillis()

        var startTime: Long = 0 // in SystemClock.uptimeMillis();

        var endTime: Long = 0 //  in SystemClock.uptimeMillis();
        var kind: String? = null
        var sql: String? = null
        var bindArgs: List<Any?>? = null
        var finished: Boolean = false
        var exception: Exception? = null
        var cookie: Int = 0
        var path: String? = null

        var resultLong: Long = Long.MIN_VALUE // MIN_VALUE means "value not set".
        var resultString: String? = null
        private val status: String
            get() = when {
                !finished -> "running"
                exception != null -> "failed"
                else -> "succeeded"
            }

        fun describe(msg: StringBuilder, allowDetailedLog: Boolean) = with(msg) {
            append(kind)
            if (finished) {
                append(" took ").append(endTime - startTime).append("ms")
            } else {
                append(" started ").append(currentTimeProvider() - startWallTime).append("ms ago")
            }
            append(" - ").append(status)
            if (sql != null) {
                append(", sql=\"").append(trimSqlForDisplay(sql)).append("\"")
            }
            if (allowDetailedLog) {
                bindArgs?.let { appendBindArgs(it) }
            }
            append(", path=").append(path)
            if (exception != null) {
                append(", exception=\"").append(exception!!.message).append("\"")
            }
            if (resultLong != Long.MIN_VALUE) {
                append(", result=").append(resultLong)
            }
            if (resultString != null) {
                append(", result=\"").append(resultString).append("\"")
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

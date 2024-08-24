/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging

import arrow.core.Either
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.FileSystemOperationError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.LoggingEvents.OperationEnd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.LoggingEvents.OperationStart
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.OperationLoggingLevel.BASIC
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.OperationLoggingLevel.NONE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.OperationLoggingLevel.VERBOSE
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import kotlin.time.Duration
import kotlin.time.measureTimedValue

public class LoggingFileSystemDecorator(
    private val delegate: FileSystem,
    private val logger: (message: () -> String) -> Unit,
    private val logEvents: LoggingEvents = LoggingEvents(),
    private val operationLevels: Map<FileSystemOperation<*, *, *>, LoggingEvents> = emptyMap(),
) : FileSystem {
    override fun <I : Any, E : FileSystemOperationError, R : Any> execute(
        operation: FileSystemOperation<I, E, R>,
        input: I,
    ): Either<E, R> {
        val loggingEvents = getLoggingEvents(operation)
        logOperationStart(loggingEvents.start, operation, input)

        val duration: Duration?
        val output: Either<E, R>
        if (loggingEvents.end.trackDuration) {
            val timedValue = measureTimedValue {
                delegate.execute(operation, input)
            }
            output = timedValue.value
            duration = timedValue.duration
        } else {
            output = delegate.execute(operation, input)
            duration = null
        }

        logOperationEnd(loggingEvents.end, operation, input, output, duration)

        return output
    }

    override fun isOperationSupported(operation: FileSystemOperation<*, *, *>): Boolean {
        return delegate.isOperationSupported(operation)
    }

    override fun close() {
        delegate.close()
    }

    private fun getLoggingEvents(operation: FileSystemOperation<*, *, *>): LoggingEvents {
        return operationLevels[operation] ?: logEvents
    }

    private fun logOperationStart(
        level: OperationStart,
        operation: FileSystemOperation<*, *, *>,
        input: Any,
    ) {
        if (level.inputs == NONE) {
            return
        }
        logger { buildOperationStartMessage(level, operation, input) }
    }

    private fun logOperationEnd(
        level: OperationEnd,
        operation: FileSystemOperation<*, *, *>,
        input: Any,
        output: Either<FileSystemOperationError, Any>,
        duration: Duration?,
    ) {
        if (level.inputs == NONE && level.outputs == NONE) {
            return
        }
        logger {
            buildOperationEndMessage(level, operation, input, output, duration)
        }
    }

    public enum class OperationLoggingLevel {
        NONE,
        NAME,
        BASIC,
        VERBOSE,
    }

    public data class LoggingEvents(
        val start: OperationStart = OperationStart(inputs = NONE),
        val end: OperationEnd = OperationEnd(),
    ) {
        public data class OperationStart(
            val inputs: OperationLoggingLevel = BASIC,
        )

        public data class OperationEnd(
            val inputs: OperationLoggingLevel = BASIC,
            val outputs: OperationLoggingLevel = BASIC,
            val trackDuration: Boolean = false,
        )
    }

    private companion object {
        private fun buildOperationStartMessage(
            level: OperationStart,
            operation: FileSystemOperation<*, *, *>,
            input: Any,
        ): String = buildString {
            append("^")
            append(operation.tag)

            if (level.inputs >= BASIC) {
                append("($input)")
            }
        }

        fun buildOperationEndMessage(
            level: OperationEnd,
            operation: FileSystemOperation<*, *, *>,
            input: Any,
            output: Either<FileSystemOperationError, Any>,
            duration: Duration?,
        ): String {
            val status = buildString {
                append(operation.tag)

                append("(): ")
                output.fold(
                    ifLeft = {
                        append(it.errno)
                        if (level.outputs >= VERBOSE) {
                            append("(${it.message})")
                        }
                    },
                    ifRight = {
                        append("OK")
                    },
                )
                append(".")
            }
            val inputsDescription: String? = if (level.inputs >= BASIC) {
                "Inputs: $input."
            } else {
                null
            }
            val outputsDescription: String? = if (level.outputs >= BASIC && output.isRight()) {
                "Outputs: ${output.getOrNull()}"
            } else {
                null
            }
            val durationDescription = duration?.let {
                "Duration: $it"
            }

            return listOfNotNull(
                status,
                inputsDescription,
                outputsDescription,
                durationDescription,
            ).joinToString(" ")
        }
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.ext

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemEngine
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl.FileSystemConfigBlock
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.dsl.FileSystemEngineConfig
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor.LoggingEvents
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor.LoggingEvents.OperationEnd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor.OperationLoggingLevel.BASIC

internal operator fun LoggingFileSystemInterceptor.Companion.invoke(
    logger: Logger,
): LoggingFileSystemInterceptor {
    return LoggingFileSystemInterceptor(
        logger = { logger.v(null, it) },
        logEvents = LoggingEvents(
            end = OperationEnd(
                inputs = BASIC,
                outputs = BASIC,
                trackDuration = false,
            ),
        ),
    )
}

@Suppress("FunctionName")
internal fun <E : FileSystemEngineConfig> DefaultFileSystem(
    engine: FileSystemEngine<E>,
    rootLogger: Logger,
    block: FileSystemConfigBlock<E>.() -> Unit = {},
): FileSystem = FileSystem(engine) {
    addInterceptor(LoggingFileSystemInterceptor(rootLogger))
    block()
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.loggin

import arrow.core.Either
import arrow.core.right
import assertk.assertThat
import assertk.assertions.containsExactly
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystemInterceptor.Chain
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadLinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor.LoggingEvents
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor.LoggingEvents.OperationEnd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor.LoggingEvents.OperationStart
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemInterceptor.OperationLoggingLevel.BASIC
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink.ReadLink
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.test.fixtures.TestFileSystem
import kotlin.test.Test

class LoggingFileSystemDecoratorTest {
    @Test
    fun decorator_should_log_success_requests_on_basic_level() {
        val delegateFs = TestFileSystem()
        delegateFs.onOperation(ReadLink) {
            "/link".right()
        }

        val loggedMessages: MutableList<String> = mutableListOf()
        val loggingInterceptor = LoggingFileSystemInterceptor(
            logger = {
                loggedMessages += it()
            },
            logEvents = LoggingEvents(
                start = OperationStart(inputs = BASIC),
                end = OperationEnd(
                    inputs = BASIC,
                    outputs = BASIC,
                    trackDuration = false,
                ),
            ),
        )
        val chain = object : Chain<ReadLink, ReadLinkError, String> {
            override val operation: FileSystemOperation<ReadLink, ReadLinkError, String> = ReadLink.Companion
            override val input: ReadLink = ReadLink(path = "/testPath", baseDirectory = CurrentWorkingDirectory)
            override fun proceed(input: ReadLink): Either<ReadLinkError, String> = "/link".right()
        }

        loggingInterceptor.intercept(chain)

        assertThat(loggedMessages).containsExactly(
            "^readlink(ReadLink(path=/testPath, baseDirectory=CurrentWorkingDirectory))",
            "readlink(): OK. Inputs: ReadLink(path=/testPath, baseDirectory=CurrentWorkingDirectory). Outputs: /link",
        )
    }
}

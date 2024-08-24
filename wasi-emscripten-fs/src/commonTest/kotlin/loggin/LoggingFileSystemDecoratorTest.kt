/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.loggin

import arrow.core.right
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isTrue
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.LoggingEvents
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.LoggingEvents.OperationEnd
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.LoggingEvents.OperationStart
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.logging.LoggingFileSystemDecorator.OperationLoggingLevel.BASIC
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.model.BaseDirectory.CurrentWorkingDirectory
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
        val loggingDecorator = LoggingFileSystemDecorator(
            delegate = delegateFs,
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
        loggingDecorator.execute(ReadLink, ReadLink(path = "/testPath", baseDirectory = CurrentWorkingDirectory))

        assertThat(loggedMessages).containsExactly(
            "^readlink(ReadLink(path=/testPath, baseDirectory=CurrentWorkingDirectory))",
            "readlink(): OK. Inputs: ReadLink(path=/testPath, baseDirectory=CurrentWorkingDirectory). Outputs: /link",
        )
    }

    @Test
    fun close_should_close_delegate() {
        var closeCalled = false
        val delegateFs = object : TestFileSystem() {
            override fun close() {
                closeCalled = true
            }
        }

        val loggingDecorator = LoggingFileSystemDecorator(
            delegate = delegateFs,
            logger = { },
        )

        loggingDecorator.close()

        assertThat(closeCalled).isTrue()
    }
}

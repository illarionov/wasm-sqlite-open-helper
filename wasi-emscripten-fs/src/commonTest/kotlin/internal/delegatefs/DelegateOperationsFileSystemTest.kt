/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.filesystem.internal.delegatefs

import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.GetCurrentWorkingDirectoryError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.NoEntry
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.error.ReadLinkError
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.FileSystemOperation
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.cwd.GetCurrentWorkingDirectory
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.opencreate.Open
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.readlink.ReadLink
import kotlin.test.Test

class DelegateOperationsFileSystemTest {
    @Test
    fun operationFileSystem_should_invoke_correct_delegates() {
        val delegateFs = DelegateOperationsFileSystem(testHandlers, emptyList())

        val cwd = delegateFs.execute(GetCurrentWorkingDirectory, Unit)
        assertThat(cwd.getOrNull()).isEqualTo("/")

        val link = delegateFs.execute(ReadLink, ReadLink("/link"))
        assertThat(link.leftOrNull())
            .isEqualTo(NoEntry("Test not entry"))
    }

    @Test
    fun operationFileSystem_test_isOperationSupported() {
        val delegateFs = DelegateOperationsFileSystem(
            testHandlers,
            emptyList(),
        )
        assertThat(delegateFs.isOperationSupported(GetCurrentWorkingDirectory)).isTrue()
        assertThat(delegateFs.isOperationSupported(ReadLink)).isTrue()
        assertThat(delegateFs.isOperationSupported(Open)).isFalse()
    }

    internal companion object {
        private val testCwdHandler: FileSystemOperationHandler<Unit, GetCurrentWorkingDirectoryError, String> =
            FileSystemOperationHandler { _ ->
                "/".right()
            }
        val testReadlinkHandler: FileSystemOperationHandler<ReadLink, ReadLinkError, String> =
            FileSystemOperationHandler { _ ->
                NoEntry("Test not entry").left()
            }
        val testHandlers: Map<FileSystemOperation<*, *, *>, FileSystemOperationHandler<*, *, *>> = mapOf(
            GetCurrentWorkingDirectory to testCwdHandler,
            ReadLink to testReadlinkHandler,
        )
    }
}

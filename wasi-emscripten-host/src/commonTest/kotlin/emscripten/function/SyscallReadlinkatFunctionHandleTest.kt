/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.emscripten.function

import arrow.core.left
import arrow.core.right
import assertk.assertThat
import assertk.assertions.isEqualTo
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.writeNullTerminatedString
import ru.pixnews.wasm.sqlite.open.helper.host.base.plus
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.ReadLink
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.op.ReadLinkError
import ru.pixnews.wasm.sqlite.open.helper.host.include.Fcntl
import ru.pixnews.wasm.sqlite.open.helper.host.test.assertions.byteAt
import ru.pixnews.wasm.sqlite.open.helper.host.test.assertions.hasBytesAt
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestMemory
import ru.pixnews.wasm.sqlite.open.helper.host.wasi.preview1.type.Errno
import ru.pixnews.wasm.sqlite.test.utils.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class SyscallReadlinkatFunctionHandleTest {
    private val fileSystem = TestFileSystem()
    private val host = TestEmbedderHost(fileSystem = fileSystem)
    private val memory = TestMemory(fileSystem = host.fileSystem)
    private val readlinkatFunctionHandle = SyscallReadlinkatFunctionHandle(host)

    @BeforeTest
    fun setup() {
        TestEnv.prepareTestEnvBeforeTest()
    }

    @AfterTest
    fun cleanup() {
        TestEnv.afterTest()
    }

    @Test
    fun readlinkAt_test_success_case() {
        val expectedLinkTarget = "usr/sbin"
        val expectedLinkTargetBytes = expectedLinkTarget.encodeToByteArray()

        fileSystem.onOperation(ReadLink) { operation ->
            assertThat(operation.path).isEqualTo("/sbin")
            expectedLinkTarget.right()
        }
        val pathnamePtr: WasmPtr<Byte> = WasmPtr(64)
        val bufPtr: WasmPtr<Byte> = WasmPtr(128)

        memory.fill(0xff.toByte())
        memory.writeNullTerminatedString(pathnamePtr, "/sbin")

        val size = readlinkatFunctionHandle.execute(
            memory = memory,
            rawDirFd = Fcntl.AT_FDCWD,
            pathnamePtr = pathnamePtr,
            buf = bufPtr,
            bufSize = 100,
        )

        assertThat(size).isEqualTo(expectedLinkTargetBytes.size)
        assertThat(memory).hasBytesAt(bufPtr, expectedLinkTargetBytes)

        // readlinkAt does not append a terminating null byte
        assertThat(memory).byteAt(bufPtr + expectedLinkTargetBytes.size).isEqualTo(0xff.toByte())
    }

    @Test
    fun readlinkAt_should_return_einval_on_incorrect_bufsize() {
        val pathnamePtr = WasmPtr<Byte>(64).also {
            memory.writeNullTerminatedString(it, "")
        }

        val sizeOrErrno = readlinkatFunctionHandle.execute(
            memory = memory,
            rawDirFd = Fcntl.AT_FDCWD,
            pathnamePtr = pathnamePtr,
            buf = WasmPtr(128),
            bufSize = -1,
        )

        assertThat(sizeOrErrno).isEqualTo(-Errno.INVAL.code)
    }

    @Test
    fun readlinkAt_should_return_negative_error_code_on_filesystem_error() {
        val pathnamePtr = WasmPtr<Byte>(64).also {
            memory.writeNullTerminatedString(it, "/")
        }
        fileSystem.onOperation(ReadLink) { _ ->
            ReadLinkError.AccessDenied("Test access denied").left()
        }

        val sizeOrErrno = readlinkatFunctionHandle.execute(
            memory = memory,
            rawDirFd = Fcntl.AT_FDCWD,
            pathnamePtr = pathnamePtr,
            buf = WasmPtr(128),
            bufSize = 100,
        )

        assertThat(sizeOrErrno).isEqualTo(-Errno.ACCES.code)
    }

    @Test
    fun readlinkAt_should_not_exceed_bufsize_limit() {
        fileSystem.onOperation(ReadLink) {
            "usr/sbin".right()
        }

        val pathnamePtr: WasmPtr<Byte> = WasmPtr(64)
        val bufPtr: WasmPtr<Byte> = WasmPtr(128)

        memory.fill(0xff.toByte())
        memory.writeNullTerminatedString(pathnamePtr, "sbin")

        val sizeOrErrno = readlinkatFunctionHandle.execute(
            memory = memory,
            rawDirFd = Fcntl.AT_FDCWD,
            pathnamePtr = pathnamePtr,
            buf = bufPtr,
            bufSize = 1,
        )

        assertThat(sizeOrErrno).isEqualTo(1)
        assertThat(memory).hasBytesAt(bufPtr, byteArrayOf('u'.code.toByte(), 0xff.toByte()))
    }
}

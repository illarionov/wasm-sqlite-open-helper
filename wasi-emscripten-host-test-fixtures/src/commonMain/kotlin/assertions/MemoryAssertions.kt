/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.assertions

import assertk.Assert
import assertk.assertions.isEqualTo
import assertk.assertions.support.appendName
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import ru.pixnews.wasm.sqlite.open.helper.host.base.WasmPtr
import ru.pixnews.wasm.sqlite.open.helper.host.base.memory.sourceWithMaxSize
import ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures.TestMemory

public fun Assert<TestMemory>.bytesAt(
    address: WasmPtr<Byte>,
    size: Int,
): Assert<ByteArray> = transform(appendName("TestMemory{$address}", separator = ".")) { testMemory ->
    testMemory.sourceWithMaxSize(address, size).buffered().use {
        it.readByteArray(size)
    }
}

public fun Assert<TestMemory>.hasBytesAt(
    address: WasmPtr<Byte>,
    expectedBytes: ByteArray,
): Unit = bytesAt(address, expectedBytes.size).isEqualTo(expectedBytes)

public fun Assert<TestMemory>.byteAt(
    address: WasmPtr<Byte>,
): Assert<Byte> = transform(appendName("TestMemory{$address}", separator = ".")) {
    it.readI8(address)
}

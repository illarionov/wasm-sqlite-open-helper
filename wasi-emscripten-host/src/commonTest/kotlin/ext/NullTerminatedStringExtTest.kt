/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.ext

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.tableOf
import kotlin.test.Test
import kotlin.test.assertFailsWith

class NullTerminatedStringExtTest {
    @Test
    fun encodeToNullTerminatedByteArray_test() {
        // TODO: correctly truncate on utf-8 character border
        tableOf("string", "maxDstSize", "result")
            .row("", 1, byteArrayOf(0))
            .row("", 2, byteArrayOf(0))
            .row("a", 1, byteArrayOf(0))
            .row("a", 2, byteArrayOf(97, 0))
            .row("a", 3, byteArrayOf(97, 0))
            .row("ab", 1, byteArrayOf(0))
            .row("ab", 2, byteArrayOf(97, 0))
            .row("ab", 3, byteArrayOf(97, 98, 0))
            .row("ab", 4, byteArrayOf(97, 98, 0))
            .forAll { str, maxLength, expectedBytes ->
                assertThat(str.encodeToNullTerminatedByteArray(maxLength))
                    .isEqualTo(expectedBytes)
            }
    }

    @Test
    fun encodeToNullTerminatedByteArray_should_fail_on_negative_arg() {
        assertFailsWith<IllegalArgumentException> { "".encodeToNullTerminatedByteArray(0) }
        assertFailsWith<IllegalArgumentException> { "".encodeToNullTerminatedByteArray(-1) }
    }

    @Test
    fun encodedNullTerminatedStringLength_test() {
        tableOf("string", "encodedLength")
            .row("", 1)
            .row("a", 2)
            .row("ab", 3)
            .forAll { str, expectedLength ->
                assertThat(str.encodedNullTerminatedStringLength())
                    .isEqualTo(expectedLength)
            }
    }
}

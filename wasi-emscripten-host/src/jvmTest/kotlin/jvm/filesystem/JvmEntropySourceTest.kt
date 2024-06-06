/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem

import assertk.assertThat
import assertk.assertions.hasSize
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEntropySource
import kotlin.test.Test
import kotlin.test.assertFails

class JvmEntropySourceTest {
    val source = JvmEntropySource()

    @Test
    fun entropySource_successTestCase_check_size() {
        val entropy = source.invoke(32)
        assertThat(entropy).hasSize(32)
    }

    @Test
    fun entropySource_should_throw_on_incorrect_arg() {
        assertFails {
            source.invoke(-1)
        }
    }
}

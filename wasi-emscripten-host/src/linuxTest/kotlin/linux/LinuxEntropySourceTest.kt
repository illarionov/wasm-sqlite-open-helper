/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.linux

import assertk.assertThat
import assertk.assertions.isFalse
import kotlin.test.Test

class LinuxEntropySourceTest {
    @Test
    @Suppress("MagicNumber")
    fun entropy_source_should_work() {
        val entropy = LinuxEntropySource.generateEntropy(102400)
        // XXX may be a more appropriate test
        assertThat(entropy.all { it == 0.toByte() }).isFalse()
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.internal

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.MonotonicClock
import kotlin.time.TimeMark
import kotlin.time.TimeSource

internal class CommonMonotonicClock(
    private val baseMark: TimeMark = TimeSource.Monotonic.markNow(),
) : MonotonicClock {
    override fun getTimeMarkNanoseconds(): Long = baseMark.elapsedNow().inWholeNanoseconds
}

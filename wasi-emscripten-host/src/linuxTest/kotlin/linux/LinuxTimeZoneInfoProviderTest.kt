/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.linux

import assertk.assertThat
import assertk.assertions.isEqualTo
import ru.pixnews.wasm.sqlite.open.helper.host.include.TimeZoneInfo
import ru.pixnews.wasm.sqlite.test.utils.withTimeZone
import kotlin.test.Test

class LinuxTimeZoneInfoProviderTest {
    @Test
    fun time_zone_provider_should_work() = withTimeZone("Europe/Paris") {
        val timeZoneInfo: TimeZoneInfo = LinuxTimeZoneInfoProvider.getTimeZoneInfo()
        assertThat(timeZoneInfo)
            .isEqualTo(
                TimeZoneInfo(
                    timeZone = -1L * 60 * 60,
                    daylight = 1,
                    stdName = "CET",
                    dstName = "CEST",
                ),
            )
    }
}

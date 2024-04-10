/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host

import ru.pixnews.wasm.sqlite.open.helper.host.include.TimeZoneInfo
import java.time.Clock
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

public class JvmTimeZoneInfoProvider(
    private val clock: Clock = Clock.systemDefaultZone(),
) : () -> TimeZoneInfo {
    private val tzFormatter = DateTimeFormatter.ofPattern("zzz", Locale.ROOT)

    override fun invoke(): TimeZoneInfo {
        val zone = clock.zone
        val now: Instant = clock.instant()

        val dayLight: Boolean
        val stdDateTime: Instant
        val dstDateTime: Instant
        val zoneRules = zone.rules
        val nextZoneTimestamp = zoneRules.nextTransition(now)?.instant
        if (nextZoneTimestamp != null) {
            dayLight = true
            val nextIsDst = !zoneRules.getDaylightSavings(nextZoneTimestamp).isZero
            if (nextIsDst) {
                stdDateTime = now
                dstDateTime = nextZoneTimestamp
            } else {
                stdDateTime = nextZoneTimestamp
                dstDateTime = now
            }
        } else {
            dayLight = false
            stdDateTime = now
            dstDateTime = now
        }

        return TimeZoneInfo(
            timeZone = zoneRules.getOffset(now).totalSeconds.toLong(),
            daylight = if (dayLight) {
                1
            } else {
                0
            },
            stdName = tzFormatter.format(stdDateTime.atZone(zone)),
            dstName = tzFormatter.format(dstDateTime.atZone(zone)),
        )
    }
}

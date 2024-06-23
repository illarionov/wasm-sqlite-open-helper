/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.linux

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.LocalTimeFormatter
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm
import kotlin.time.Duration

internal class LinuxLocalTimeFormatter(
) : LocalTimeFormatter {
    override fun format(epoch: Duration): StructTm {
        // TODO:
        error("Not implemented")
//        val instant = Instant.ofEpochMilli(epoch.inWholeMilliseconds)
//
//        val date: ZonedDateTime = ZonedDateTime.ofInstant(
//            instant,
//            localTimeZoneProvider(),
//        )
//        val zone = date.zone
//        return StructTm(
//            tm_sec = date.second,
//            tm_min = date.minute,
//            tm_hour = date.hour,
//            tm_mday = date.dayOfMonth,
//            tm_mon = date.monthValue - 1,
//            tm_year = date.year - 1900,
//            tm_wday = date.dayOfWeek.value % 7,
//            tm_yday = date.dayOfYear - 1,
//            tm_isdst = if (date.zone.rules.isDaylightSavings(date.toInstant())) {
//                StructTm.IsDstFlag.IN_EFFECT
//            } else {
//                StructTm.IsDstFlag.NOT_IN_EFFECT
//            }.asTmIsdstValue(),
//            tm_gmtoff = zone.rules.getOffset(date.toInstant()).totalSeconds,
//            tm_zone = zone.id,
//        )
    }
}

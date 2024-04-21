/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.chicory

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.JvmLocalTimeFormatter
import ru.pixnews.wasm.sqlite.open.helper.host.JvmTimeZoneInfoProvider
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm
import ru.pixnews.wasm.sqlite.open.helper.host.include.TimeZoneInfo
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

// TODO: merge
public class SqliteEmbedderHost(
    public val rootLogger: Logger = Logger,
    public val systemEnvProvider: () -> Map<String, String> = System::getenv,
    public val commandArgsProvider: () -> List<String> = ::emptyList,
    public val fileSystem: FileSystem = FileSystem(rootLogger),
    public val clock: () -> Duration = { System.currentTimeMillis().milliseconds },
    public val monotonicClock: () -> Duration = { System.nanoTime().nanoseconds },
    public val localTimeFormatter: (Duration) -> StructTm = JvmLocalTimeFormatter(),
    public val timeZoneInfo: () -> TimeZoneInfo = JvmTimeZoneInfoProvider(),
)

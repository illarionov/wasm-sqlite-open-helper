/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm
import ru.pixnews.wasm.sqlite.open.helper.host.include.TimeZoneInfo
import ru.pixnews.wasm.sqlite.test.utils.KermitLogger
import kotlin.time.Duration

open class TestEmbedderHost(
    override var rootLogger: Logger = KermitLogger(),
    override var systemEnvProvider: () -> Map<String, String> = { emptyMap() },
    override var commandArgsProvider: () -> List<String> = { emptyList() },
    override var fileSystem: FileSystem<*> = TestFileSystem(),
    override var clock: () -> Duration = { Duration.INFINITE },
    override var monotonicClock: () -> Duration = { Duration.INFINITE },
    override var localTimeFormatter: (Duration) -> StructTm = {
        StructTm(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1)
    },
    override var timeZoneInfo: () -> TimeZoneInfo = {
        TimeZoneInfo(-1, -1, "Dummy", "Dummy")
    },
) : EmbedderHost

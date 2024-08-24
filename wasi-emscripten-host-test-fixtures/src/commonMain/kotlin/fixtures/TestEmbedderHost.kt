/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.test.fixtures

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.Clock
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.CommandArgsProvider
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.EntropySource
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.LocalTimeFormatter
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.MonotonicClock
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.SystemEnvProvider
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.TimeZoneInfoProvider
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.test.fixtures.TestFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm
import ru.pixnews.wasm.sqlite.open.helper.host.include.TimeZoneInfo
import ru.pixnews.wasm.sqlite.test.utils.KermitLogger

public open class TestEmbedderHost(
    override var rootLogger: Logger = KermitLogger(),
    override var systemEnvProvider: SystemEnvProvider = SystemEnvProvider { emptyMap() },
    override var commandArgsProvider: CommandArgsProvider = CommandArgsProvider { emptyList() },
    override var fileSystem: FileSystem = TestFileSystem(),
    override var monotonicClock: MonotonicClock = MonotonicClock { Long.MAX_VALUE },
    override var clock: Clock = Clock { Long.MAX_VALUE },
    override var localTimeFormatter: LocalTimeFormatter = LocalTimeFormatter {
        StructTm(-1, -1, -1, -1, -1, -1, -1, -1, -1, -1)
    },
    override var timeZoneInfo: TimeZoneInfoProvider = TimeZoneInfoProvider {
        TimeZoneInfo(-1, -1, "Dummy", "Dummy")
    },
    override var entropySource: EntropySource = EntropySource { size ->
        ByteArray(size) { 4 }
    },
) : EmbedderHost

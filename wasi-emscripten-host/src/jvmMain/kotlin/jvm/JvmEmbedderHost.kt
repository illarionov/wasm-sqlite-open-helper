/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm

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
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.JvmFileSystem

public class JvmEmbedderHost(
    public override val rootLogger: Logger = Logger,
    public override val systemEnvProvider: SystemEnvProvider = JvmSystemEnvProvider,
    public override val commandArgsProvider: CommandArgsProvider = JvmCommandArgsProvider,
    public override val fileSystem: FileSystem<*> = JvmFileSystem(rootLogger),
    public override val clock: Clock = JvmClock,
    public override val monotonicClock: MonotonicClock = JvmMonotonicClock,
    public override val localTimeFormatter: LocalTimeFormatter = JvmLocalTimeFormatter(),
    public override val timeZoneInfo: TimeZoneInfoProvider = JvmTimeZoneInfoProvider(),
    public override val entropySource: EntropySource = JvmEntropySource(),
) : EmbedderHost {
    internal object JvmSystemEnvProvider : SystemEnvProvider {
        override fun getSystemEnv(): Map<String, String> = System.getenv()
    }

    internal object JvmCommandArgsProvider : CommandArgsProvider {
        override fun getCommandArgs(): List<String> = emptyList()
    }

    internal object JvmClock : Clock {
        override fun getCurrentTimeEpochMilliseconds(): Long = System.currentTimeMillis()
    }

    internal object JvmMonotonicClock : MonotonicClock {
        override fun getTimeMarkNanoseconds(): Long = System.nanoTime()
    }
}

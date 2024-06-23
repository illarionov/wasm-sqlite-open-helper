/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.linux

import kotlin.time.Duration
import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.CommandArgsProvider
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.EntropySource
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.LocalTimeFormatter
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.MonotonicClock
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.SystemEnvProvider
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.TimeZoneInfoProvider
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.linux.filesystem.PosixFileSystem

public class LinuxEmbedderHost(
    override val rootLogger: Logger,
    override val systemEnvProvider: SystemEnvProvider = NativeSystemEnvProvider,
    override val commandArgsProvider: CommandArgsProvider = NativeCommandArgsProvider,
    override val fileSystem: FileSystem<*> = PosixFileSystem(rootLogger),
    override val monotonicClock: MonotonicClock = NativeMonotonicClock,
    override val clock: EmbedderHost.Clock = NativeClock,
    override val localTimeFormatter: LocalTimeFormatter = LinuxLocalTimeFormatter(),
    override val timeZoneInfo: TimeZoneInfoProvider = LinuxTimeZoneInfoProvider(),
    override val entropySource: EntropySource = LinuxEntropySource(),
) : EmbedderHost {
    internal object NativeSystemEnvProvider : SystemEnvProvider {
        override fun getSystemEnv(): Map<String, String> = emptyMap() // TODO:
    }

    internal object NativeCommandArgsProvider : CommandArgsProvider {
        override fun getCommandArgs(): List<String> = emptyList()
    }

    internal object NativeClock : EmbedderHost.Clock {
        override fun getCurrentTime(): Duration = Duration.ZERO // TODO
    }

    internal object NativeMonotonicClock : MonotonicClock {
        override fun getTimeMark(): Duration = Duration.ZERO
    }
}

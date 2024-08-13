/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.linux

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
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.NotImplementedFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonClock
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonMonotonicClock

public class LinuxEmbedderHost(
    override val rootLogger: Logger,
    override val systemEnvProvider: SystemEnvProvider = NativeSystemEnvProvider,
    override val commandArgsProvider: CommandArgsProvider = NativeCommandArgsProvider,
    override val fileSystem: FileSystem = NotImplementedFileSystem,
    override val monotonicClock: MonotonicClock = CommonMonotonicClock(),
    override val clock: Clock = CommonClock(),
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
}

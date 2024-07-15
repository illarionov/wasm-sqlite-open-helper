/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.apple

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.stub.NotImplementedFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonClock
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonMonotonicClock

public class AppleEmbedderHost(
    override val rootLogger: Logger,
    override val systemEnvProvider: EmbedderHost.SystemEnvProvider = AppleSystemEnvProvider,
    override val commandArgsProvider: EmbedderHost.CommandArgsProvider = AppleCommandArgsProvider,
    override val fileSystem: FileSystem<*> = NotImplementedFileSystem(rootLogger),
    override val monotonicClock: EmbedderHost.MonotonicClock = CommonMonotonicClock(),
    override val clock: EmbedderHost.Clock = CommonClock(),
    override val localTimeFormatter: EmbedderHost.LocalTimeFormatter = AppleLocalTimeFormatter(),
    override val timeZoneInfo: EmbedderHost.TimeZoneInfoProvider = AppleTimeZoneInfoProvider(),
    override val entropySource: EmbedderHost.EntropySource = AppleEntropySource(),
) : EmbedderHost {
    internal object AppleSystemEnvProvider : EmbedderHost.SystemEnvProvider {
        override fun getSystemEnv(): Map<String, String> = emptyMap() // TODO:
    }

    internal object AppleCommandArgsProvider : EmbedderHost.CommandArgsProvider {
        override fun getCommandArgs(): List<String> = emptyList()
    }
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.Builder
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEmbedderHost.JvmClock
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEmbedderHost.JvmCommandArgsProvider
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEmbedderHost.JvmMonotonicClock
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEmbedderHost.JvmSystemEnvProvider
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmEntropySource
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmLocalTimeFormatter
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.JvmTimeZoneInfoProvider
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.nio.JvmNioFileSystem

internal actual fun createDefaultEmbedderHost(builder: Builder): EmbedderHost = JvmEmbedderHost(
    rootLogger = builder.rootLogger,
    systemEnvProvider = builder.systemEnvProvider ?: JvmSystemEnvProvider,
    commandArgsProvider = builder.commandArgsProvider ?: JvmCommandArgsProvider,
    fileSystem = builder.fileSystem ?: JvmNioFileSystem(rootLogger = builder.rootLogger),
    clock = builder.clock ?: JvmClock,
    monotonicClock = builder.monotonicClock ?: JvmMonotonicClock,
    localTimeFormatter = builder.localTimeFormatter ?: JvmLocalTimeFormatter(),
    timeZoneInfo = builder.timeZoneInfo ?: JvmTimeZoneInfoProvider(),
    entropySource = builder.entropySource ?: JvmEntropySource(),
)

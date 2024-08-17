/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host

import ru.pixnews.wasm.sqlite.open.helper.host.apple.AppleEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.apple.AppleEmbedderHost.AppleCommandArgsProvider
import ru.pixnews.wasm.sqlite.open.helper.host.apple.AppleEmbedderHost.AppleSystemEnvProvider
import ru.pixnews.wasm.sqlite.open.helper.host.apple.AppleEntropySource
import ru.pixnews.wasm.sqlite.open.helper.host.apple.AppleLocalTimeFormatter
import ru.pixnews.wasm.sqlite.open.helper.host.apple.AppleTimeZoneInfoProvider
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.NotImplementedFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonClock
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonMonotonicClock

internal actual fun createDefaultEmbedderHost(builder: EmbedderHost.Builder): EmbedderHost = AppleEmbedderHost(
    rootLogger = builder.rootLogger,
    systemEnvProvider = builder.systemEnvProvider ?: AppleSystemEnvProvider,
    commandArgsProvider = builder.commandArgsProvider ?: AppleCommandArgsProvider,
    fileSystem = builder.fileSystem ?: NotImplementedFileSystem,
    monotonicClock = builder.monotonicClock ?: CommonMonotonicClock(),
    clock = builder.clock ?: CommonClock(),
    localTimeFormatter = builder.localTimeFormatter ?: AppleLocalTimeFormatter(),
    timeZoneInfo = builder.timeZoneInfo ?: AppleTimeZoneInfoProvider(),
    entropySource = builder.entropySource ?: AppleEntropySource(),
)

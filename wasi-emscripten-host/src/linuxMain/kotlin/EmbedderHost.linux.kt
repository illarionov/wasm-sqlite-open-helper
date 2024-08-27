/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host

import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost.Builder
import ru.pixnews.wasm.sqlite.open.helper.host.ext.DefaultFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.LinuxFileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonClock
import ru.pixnews.wasm.sqlite.open.helper.host.internal.CommonMonotonicClock
import ru.pixnews.wasm.sqlite.open.helper.host.internal.EmptyCommandArgsProvider
import ru.pixnews.wasm.sqlite.open.helper.host.linux.LinuxEmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.linux.LinuxEntropySource
import ru.pixnews.wasm.sqlite.open.helper.host.linux.LinuxLocalTimeFormatter
import ru.pixnews.wasm.sqlite.open.helper.host.linux.LinuxSystemEnvProvider
import ru.pixnews.wasm.sqlite.open.helper.host.linux.LinuxTimeZoneInfoProvider

internal actual fun createDefaultEmbedderHost(builder: Builder): EmbedderHost = LinuxEmbedderHost(
    rootLogger = builder.rootLogger,
    systemEnvProvider = builder.systemEnvProvider ?: LinuxSystemEnvProvider,
    commandArgsProvider = builder.commandArgsProvider ?: EmptyCommandArgsProvider,
    fileSystem = builder.fileSystem ?: DefaultFileSystem(LinuxFileSystem, builder.rootLogger.withTag("FSlnx")),
    monotonicClock = builder.monotonicClock ?: CommonMonotonicClock(),
    clock = builder.clock ?: CommonClock(),
    localTimeFormatter = builder.localTimeFormatter ?: LinuxLocalTimeFormatter,
    timeZoneInfo = builder.timeZoneInfo ?: LinuxTimeZoneInfoProvider,
    entropySource = builder.entropySource ?: LinuxEntropySource,
)

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.jvm

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.EmbedderHost
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm
import ru.pixnews.wasm.sqlite.open.helper.host.include.TimeZoneInfo
import ru.pixnews.wasm.sqlite.open.helper.host.jvm.filesystem.JvmFileSystem
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

public class JvmEmbedderHost(
    public override val rootLogger: Logger = Logger,
    public override val systemEnvProvider: () -> Map<String, String> = System::getenv,
    public override val commandArgsProvider: () -> List<String> = ::emptyList,
    public override val fileSystem: FileSystem<*> = JvmFileSystem(rootLogger),
    public override val clock: () -> Duration = { System.currentTimeMillis().milliseconds },
    public override val monotonicClock: () -> Duration = { System.nanoTime().nanoseconds },
    public override val localTimeFormatter: (Duration) -> StructTm = JvmLocalTimeFormatter(),
    public override val timeZoneInfo: () -> TimeZoneInfo = JvmTimeZoneInfoProvider(),
    public override val entropySource: (size: Int) -> ByteArray = JvmEntropySource(),
) : EmbedderHost

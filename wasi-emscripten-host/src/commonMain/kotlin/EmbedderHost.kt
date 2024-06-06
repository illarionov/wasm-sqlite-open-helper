/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.FileSystem
import ru.pixnews.wasm.sqlite.open.helper.host.filesystem.Path
import ru.pixnews.wasm.sqlite.open.helper.host.include.StructTm
import ru.pixnews.wasm.sqlite.open.helper.host.include.TimeZoneInfo
import kotlin.time.Duration

public interface EmbedderHost {
    public val rootLogger: Logger
    public val systemEnvProvider: () -> Map<String, String>
    public val commandArgsProvider: () -> List<String>
    public val fileSystem: FileSystem<*>
    public val clock: () -> Duration
    public val monotonicClock: () -> Duration
    public val localTimeFormatter: (Duration) -> StructTm
    public val timeZoneInfo: () -> TimeZoneInfo
    public val entropySource: (size: Int) -> ByteArray
}

@Suppress("UNCHECKED_CAST")
public fun <P : Path> EmbedderHost.castFileSystem(): FileSystem<P> = fileSystem as FileSystem<P>

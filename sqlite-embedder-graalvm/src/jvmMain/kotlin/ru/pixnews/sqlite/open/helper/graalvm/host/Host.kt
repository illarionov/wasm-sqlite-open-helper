/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.graalvm.host

import ru.pixnews.sqlite.open.helper.host.filesystem.FileSystem
import java.time.Clock

internal class Host(
    val systemEnvProvider: () -> Map<String, String> = System::getenv,
    val commandArgsProvider: () -> List<String> = ::emptyList,
    val fileSystem: FileSystem = FileSystem(),
    val clock: Clock = Clock.systemDefaultZone(),
)

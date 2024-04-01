/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage", "GENERIC_VARIABLE_WRONG_DECLARATION")

import ru.pixnews.wasm.sqlite.open.helper.builder.icu.internal.createIcuSourceConfiguration
import ru.pixnews.wasm.sqlite.open.helper.builder.icu.internal.setupUnpackingIcuAttributes

// Convention Plugin for building ICU for WASM using Emscripten
plugins {
    base
}

setupUnpackingIcuAttributes()

internal val icuSources = createIcuSourceConfiguration(
    icuVersion = versionCatalogs.named("libs").findVersion("icu").get().toString(),
)

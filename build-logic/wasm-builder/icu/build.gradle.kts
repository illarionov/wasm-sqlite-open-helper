/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    `kotlin-dsl`
}

group = "ru.pixnews.wasm.builder.icu"

kotlin {
    explicitApi = ExplicitApiMode.Warning
}

dependencies {
    api(projects.base)
}

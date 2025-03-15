/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `kotlin-dsl`
}

group = "at.released.wasm-sqlite-driver.gradle.documentation"

dependencies {
    implementation(libs.dokka.plugin)
    implementation(libs.node.plugin)
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    `kotlin-dsl`
}

group = "ru.pixnews.wasm-sqlite-open-helper.gradle.multiplatform"

dependencies {
    implementation(project(":lint"))
    implementation(libs.agp.plugin.api)
    implementation(libs.atomicfu.plugin)
    implementation(libs.ksp.plugin)
    implementation(libs.dokka.plugin)
    implementation(libs.gradle.maven.publish.plugin)
    implementation(libs.kotlin.gradle.plugin)
    runtimeOnly(libs.agp.plugin)
}

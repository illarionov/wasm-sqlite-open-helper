/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform

import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

/*
 * Convention plugin that acivates kotlin multiplatform resources
 */
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        sourceSets {
            commonMain.dependencies {
                compileOnly(compose.dependencies.runtime)
            }
            commonTest.dependencies {
                compileOnly(compose.dependencies.runtime)
            }
        }
    }
}

composeCompiler {
    targetKotlinPlatforms.set(emptySet())
}

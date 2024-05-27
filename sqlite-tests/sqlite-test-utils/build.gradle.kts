/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
}

group = "ru.pixnews.wasm-sqlite-open-helper"

kotlin {
    jvm()
    linuxX64()
    js {
        browser()
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            api(kotlin("test"))
            api(libs.assertk)
            api(libs.kermit)
            api(project.dependencies.platform(libs.kotlinx.coroutines.bom))
            api(projects.commonApi)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

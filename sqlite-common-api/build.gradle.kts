/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.sqlite.open.helper.gradle.multiplatform.kotlin")
}

group = "ru.pixnews.sqlite.open.helper.sqlite.common.api"

kotlin {
    jvm()
    linuxX64()

    sourceSets {
        commonMain.dependencies {
            api(projects.commonApi)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.assertk)
        }
    }
}
/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.google.devtools.ksp")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
}

group = "ru.pixnews.wasm-sqlite-open-helper"

kotlin {
    jvm {
       withJava()
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.sqlite.sqlite)
            api(projects.sqliteCommon)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    add("annotationProcessor", libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
}

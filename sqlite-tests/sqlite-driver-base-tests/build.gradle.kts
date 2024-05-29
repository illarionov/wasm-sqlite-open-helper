import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("com.google.devtools.ksp")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin-with-java")
}

group = "ru.pixnews.wasm-sqlite-open-helper"

kotlin {
    explicitApi = ExplicitApiMode.Disabled

    sourceSets {
        commonMain.dependencies {
            api(kotlin("test-common"))
            api(kotlin("test-annotations-common"))
            api(libs.androidx.sqlite.sqlite)
            api(projects.sqliteCommon)
            api(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.testing)
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            compileOnly(kotlin("test-junit5"))
        }
    }
}

dependencies {
    add("annotationProcessor", libs.androidx.room.compiler)
    ksp(libs.androidx.room.compiler)
}

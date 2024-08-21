import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.atomicfu")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_wasi_emscripten_fs_version",
    envVariableName = "WSOH_WASI_EMSCRIPTEN_FS_VERSION",
).get()

kotlin {
    jvm()
    iosSimulatorArm64()
    iosArm64()
    iosX64()
    linuxArm64 {
        // TODO: compile interops for arm64
    }
    linuxX64 {
        setupLinuxInterops()
    }
    macosArm64()
    macosX64()
    mingwX64()

    sourceSets {
        commonMain.dependencies {
            api(projects.commonApi)
            implementation(projects.commonLock)
            api(libs.arrow.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(libs.assertk)
        }
    }
}

fun KotlinNativeTarget.setupLinuxInterops() = compilations.named("main") {
    cinterops {
        create("atfile") {
            packageName("ru.pixnews.wasm.sqlite.open.helper.host.platform.linux")
            compilerOpts("-I/usr/include/x86_64-linux-gnu", "-I/usr/include")
        }
    }
}

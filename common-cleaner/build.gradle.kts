/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("OPT_IN_USAGE")

plugins {
    id("at.released.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.android")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("at.released.wasm.sqlite.open.helper.gradle.multiplatform.publish")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_common_cleaner_version",
    envVariableName = "WSOH_COMMON_CLEANER_VERSION",
).get()

android {
    namespace = "at.released.wasm.sqlite.open.helper.common.cleaner"
    defaultConfig {
        minSdk = 14
    }
}

kotlin {
    androidTarget()
    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    wasmJs {
        browser()
        nodejs()
    }
    iosSimulatorArm64()
    iosArm64()
    iosX64()
    linuxArm64()
    linuxX64()
    macosArm64()
    macosX64()
    mingwX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        val jvmAndAndroid by creating {
            dependsOn(commonMain.get())
        }
        androidMain.get().dependsOn(jvmAndAndroid)
        jvmMain.get().dependsOn(jvmAndAndroid)
    }
}

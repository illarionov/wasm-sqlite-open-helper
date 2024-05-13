/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform

import com.android.build.api.dsl.LibraryExtension

/*
 * Convention plugin that configures Android target in projects with the Kotlin Multiplatform plugin
 */
plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.android-lint")
    id("com.android.library")
}

kotlin {
    androidTarget {
        publishLibraryVariants = listOf("release")
    }
}

extensions.configure<LibraryExtension>("android") {
    compileSdk = versionCatalogs.named("libs").findVersion("androidCompileSdk").get().displayName.toInt()
    defaultConfig {
        minSdk = versionCatalogs.named("libs").findVersion("androidMinSdk").get().displayName.toInt()
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }
}

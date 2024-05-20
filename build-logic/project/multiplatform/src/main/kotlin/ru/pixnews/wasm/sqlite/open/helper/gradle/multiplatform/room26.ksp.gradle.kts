/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform

import com.android.build.api.dsl.CommonExtension
import org.gradle.kotlin.dsl.configure

/*
 * Convention plugin that configures Room without using the Room Gradle plugin
 * for use with Room 2.6.x
 */
plugins {
    id("com.google.devtools.ksp")
}

extensions.configure(CommonExtension::class.java) {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.generateKotlin" to "true",
                )
            }
        }
    }
}

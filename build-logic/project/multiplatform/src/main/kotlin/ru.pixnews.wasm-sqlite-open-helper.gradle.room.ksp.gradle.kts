/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import androidx.room.gradle.RoomExtension
import com.android.build.api.dsl.CommonExtension

/*
 * Convention plugin that configures Room
 */
plugins {
    id("androidx.room")
    id("com.google.devtools.ksp")
}

extensions.configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas/")
}

extensions.configure<CommonExtension<*, *, *, *, *>>("android") {
    defaultConfig {
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.generateKotlin" to "true",
                    "room.incremental" to "true",
                )
            }
        }
    }
}

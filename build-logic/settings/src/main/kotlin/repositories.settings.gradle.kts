/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.gradle.settings

import ru.pixnews.wasm.sqlite.open.helper.gradle.settings.repository.chicoryMavenLocal
import ru.pixnews.wasm.sqlite.open.helper.gradle.settings.repository.googleFiltered
import ru.pixnews.wasm.sqlite.open.helper.gradle.settings.repository.pixnewsMaven
import ru.pixnews.wasm.sqlite.open.helper.gradle.settings.repository.pixnewsMavenLocal

/*
 * Settings convention plugin that configures repositories used in the application
 */
pluginManagement {
    repositories {
        googleFiltered()
        mavenCentral()
        gradlePluginPortal()
    }

    // Get our own convention plugins from 'gradle/plugin/project'
    listOf(
        "project" to "sqlite-helper-gradle-project-plugins",
    ).forEach { (path, gradleProjectsPluginName) ->
        if (File(rootDir, "build-logic/$path").exists()) {
            includeBuild("build-logic/$path") {
                name = gradleProjectsPluginName
            }
        }
        // If not the main build, 'project' is located next to the build
        if (File(rootDir, "../$path").exists()) {
            includeBuild("../$path") {
                name = gradleProjectsPluginName
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        pixnewsMavenLocal()
        chicoryMavenLocal()
        mavenCentral()
        googleFiltered()
        pixnewsMaven()
    }
}

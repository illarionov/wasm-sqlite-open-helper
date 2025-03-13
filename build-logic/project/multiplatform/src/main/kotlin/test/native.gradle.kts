/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.gradle.multiplatform.test

import at.released.wasm.sqlite.open.helper.gradle.multiplatform.ext.capitalizeAscii
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.ExecutionTaskHolder
import org.jetbrains.kotlin.gradle.targets.native.KotlinNativeHostTestRun
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest

/*
 * Convention plugin that configures tests for native targets in projects with the Kotlin Multiplatform plugin
 */
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    extensions.configure<KotlinMultiplatformExtension> {
        linuxX64 {
            testRuns.configureEach {
                setupXdgHome()
            }
        }
    }
}

fun KotlinNativeHostTestRun.setupXdgHome() {
    val xdgHome = setupXdgHomeTask(this)
    @Suppress("UNCHECKED_CAST")
    (this as ExecutionTaskHolder<KotlinNativeTest>).executionTask.configure {
        environment("XDG_DATA_HOME", xdgHome.xdgDataHome.get().asFile.absolutePath)
        environment("XDG_DATA_DIRS", "")
        dependsOn(xdgHome.prepareRootTask)
    }
}

fun setupXdgHomeTask(
    run: KotlinNativeHostTestRun,
    appName: String = "wasm-sqlite-open-helper",
): XdgHome {
    val runName = run.name
    val root = layout.buildDirectory.dir("testXdgHome/$runName")
    val xdgDataHomeRoot = root.map { it.dir(".local/share") }
    val appDataRoot = xdgDataHomeRoot.map { it.dir(appName) }
    val mainResourcesDir = run.target.compilations["main"].output.resourcesDirProvider

    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val buildTask = tasks.register<Sync>("prepareTestXdgHome${runName.capitalizeAscii()}") {
        from(mainResourcesDir)
        into(appDataRoot)
    }
    return XdgHome(
        root = root,
        xdgDataHome = xdgDataHomeRoot,
        prepareRootTask = buildTask,
    )
}

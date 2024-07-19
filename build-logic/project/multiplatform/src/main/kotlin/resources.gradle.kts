/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform

import org.jetbrains.kotlin.gradle.ComposeKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.plugin.mpp.resources.KotlinTargetResourcesPublication
import ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.ext.capitalizeAscii

/*
 * Convention plugin that activates kotlin multiplatform resources
 *
 * Source: https://github.com/JetBrains/compose-multiplatform/blob/5da48904e5c24aa567da17fb780707073f920af3/gradle-plugins/compose/src/main/kotlin/org/jetbrains/compose/resources/KmpResources.kt
 */
@OptIn(ComposeKotlinGradlePluginApi::class)
plugins.withId("org.jetbrains.kotlin.multiplatform") {
    val platformsForSetupCompilation = setOf(KotlinPlatformType.native, KotlinPlatformType.js, KotlinPlatformType.wasm)
    val kotlinExtension: KotlinMultiplatformExtension = extensions.getByType()
    val kmpResources = extraProperties.get("multiplatformResourcesPublication") as KotlinTargetResourcesPublication

    kotlinExtension.targets
        .matching { target -> target.platformType in platformsForSetupCompilation }
        .configureEach {
            val allResources = kmpResources.resolveResources(this)
            compilations.all {
                if (this.name == "main") {
                    configureResourcesForCompilation(this, allResources)
                }
            }
        }
    configureAppleTestResources(kotlinExtension)
}

/**
 * Add resolved resources to a kotlin compilation to include it into a resulting platform artefact
 * It is required for JS and Native targets.
 * For JVM and Android it works automatically via jar files
 */
private fun configureResourcesForCompilation(
    compilation: KotlinCompilation<*>,
    directoryWithAllResourcesForCompilation: Provider<File>,
) {
    compilation.defaultSourceSet.resources.srcDir(directoryWithAllResourcesForCompilation)

    // JS packaging requires explicit dependency
    if (compilation is KotlinJsCompilation) {
        tasks.named(compilation.processResourcesTaskName).configure {
            dependsOn(directoryWithAllResourcesForCompilation)
        }
    }
}

/**
 * Place resources into the test binary's output directory on Apple platforms so that they can be accessed using
 * NSBundle.mainBundle.
 */
private fun configureAppleTestResources(
    kotlinExtension: KotlinMultiplatformExtension,
) {
    val appleTargetsWithResources = setOf("iosSimulatorArm64", "iosArm64", "iosX64", "macosArm64", "macosX64")
    kotlinExtension.targets
        .withType(KotlinNativeTarget::class.java)
        .matching { target -> target.name in appleTargetsWithResources }
        .configureEach {
            configureCopyTestResources(this)
        }
}

private fun configureCopyTestResources(
    nativeTarget: KotlinNativeTarget,
) {
    @Suppress("GENERIC_VARIABLE_WRONG_DECLARATION")
    val copyResourcesTask = tasks.register<Copy>(
        "copyTestComposeResourcesFor${nativeTarget.name.capitalizeAscii()}",
    )

    nativeTarget.binaries.withType(TestExecutable::class.java).all {
        val testExec = this
        val resourcesDirectories: Provider<List<SourceDirectorySet>> = provider {
            (testExec.compilation.associatedCompilations + testExec.compilation).flatMap {
                it.allKotlinSourceSets.map(KotlinSourceSet::resources)
            }
        }
        copyResourcesTask.configure {
            from(resourcesDirectories)
            into(testExec.outputDirectory.resolve("wsoh-resources"))
        }

        testExec.linkTaskProvider.configure {
            dependsOn(copyResourcesTask)
        }
    }
}

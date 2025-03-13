/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.LibraryVariantBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable

plugins {
    id("com.google.devtools.ksp")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.lint.binary-compatibility-validator")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.android")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.android-instrumented-test")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.test.jvm")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.test.native")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.atomicfu")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.kotlin")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.publish")
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.multiplatform.resources")
}

group = "ru.pixnews.wasm-sqlite-open-helper"
version = wasmSqliteVersions.getSubmoduleVersionProvider(
    propertiesFileKey = "wsoh_sqlite_driver_version",
    envVariableName = "WSOH_SQLITE_DRIVER_VERSION",
).get()

android {
    namespace = "ru.pixnews.wasm.sqlite.driver"
    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            testProguardFiles += listOf(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                file("proguard-test-rules.pro"),
            )
            androidTest {
                enableMinification = true
            }
        }
    }
    testOptions {
        unitTests {
            isReturnDefaultValues = false
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources.excludes += listOf(
            "META-INF/LICENSE.md",
            "META-INF/LICENSE-notice.md",
        )
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    testBuildType = "release"

    androidComponents {
        beforeVariants(selector().withBuildType("release")) { variantBuilder: LibraryVariantBuilder ->
            variantBuilder.hostTests.forEach { (_, builder) -> builder.enable = false }
        }
    }
}

kotlin {
    androidTarget()
    iosSimulatorArm64()
    iosArm64()
    iosX64()
    jvm()
    linuxX64 {
        binaries.withType(TestExecutable::class.java).all {
            debuggable = false
            optimized = true
        }
    }
    linuxArm64()
    macosArm64()
    macosX64()

    applyDefaultHierarchyTemplate()

    sourceSets {
        androidMain.dependencies {
        }
        androidUnitTest.dependencies {
            implementation(libs.androidx.test.core)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.rules)
            implementation(libs.chicory.runtime)
            implementation(libs.wsoh.sqlite.mt)
            implementation(libs.wsoh.sqlite.mt.plain)
            implementation(libs.wsoh.sqlite.st)
            implementation(libs.wsoh.sqlite.st.aot)
            implementation(libs.wsoh.sqlite.st.plain)
            implementation(projects.sqliteEmbedderChasm)
            implementation(projects.sqliteEmbedderChicory)
            implementation(projects.sqliteEmbedderGraalvm)
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(projects.sqliteTests.sqliteDriverBaseTests)
        }

        commonMain.dependencies {
            api(projects.commonApi)
            api(projects.sqliteCommon)
            api(libs.cassettes.playhead)
            api(libs.androidx.sqlite.sqlite)
            implementation(projects.commonCleaner)
            implementation(libs.wasi.emscripten.host)
        }

        listOf(
            linuxMain,
            macosMain,
            iosMain,
        ).forEach { target ->
            target.dependencies {
                // TODO: Shouldn't be here, added for resources in tests
                // https://github.com/JetBrains/compose-multiplatform/issues/4442
                implementation(libs.wsoh.sqlite.st)
                implementation(libs.wsoh.sqlite.st.plain)
            }
        }

        commonTest.dependencies {
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(projects.sqliteTests.sqliteDriverBaseTests)
            implementation(projects.sqliteEmbedderChasm)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.testing)
            implementation(libs.kermit)
            implementation(libs.wsoh.sqlite.st)
            implementation(libs.wsoh.sqlite.st.plain)
            implementation(libs.wasi.emscripten.host.test.logger)
        }

        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
        }
        androidMain {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                implementation(libs.atomicfu)
            }
        }
        jvmMain.get().dependsOn(jvmAndAndroidMain)

        val jvmAndAndroidTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                kotlin("test-junit")
                implementation(libs.wsoh.sqlite.mt)
                implementation(libs.wsoh.sqlite.mt.plain)
                implementation(libs.wsoh.sqlite.st.aot)
                implementation(libs.androidx.sqlite.bundled)
                implementation(projects.sqliteEmbedderChicory)
                implementation(projects.sqliteEmbedderGraalvm)
                implementation(projects.sqliteTests.sqliteDriverBaseTests)
            }
        }
        androidUnitTest.get().dependsOn(jvmAndAndroidTest)
        jvmTest {
            dependsOn(jvmAndAndroidTest)
            dependencies {
                implementation(libs.chicory.aot)
                implementation(libs.kotlinx.coroutines.debug)
                implementation(libs.mockk)
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    constraints {
        listOf(
            "at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-349:*",
            "at.released.wasm-sqlite-driver:sqlite-android-wasm-emscripten-icu-mt-pthread-349:*",
        ).forEach { dependency ->
            testImplementation(dependency) {
                attributes {
                    attribute(
                        KotlinPlatformType.attribute,
                        KotlinPlatformType.jvm,
                    )
                }
            }
        }
    }
}

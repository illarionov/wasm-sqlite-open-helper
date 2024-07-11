/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

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
}

kotlin {
    androidTarget()
    jvm()
    linuxX64()

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
            implementation(libs.wsoh.sqlite.mt)
            implementation(libs.wsoh.sqlite.st)
            implementation(projects.sqliteEmbedderChasm)
            implementation(projects.sqliteEmbedderChicory)
            implementation(projects.sqliteEmbedderGraalvm)
            implementation(projects.sqliteTests.sqliteTestUtils)
            implementation(projects.sqliteTests.sqliteDriverBaseTests)
        }

        commonMain.dependencies {
            api(projects.commonApi)
            api(projects.sqliteCommon)
            api(libs.androidx.sqlite.sqlite)
            implementation(projects.commonCleaner)
            implementation(projects.commonLock)
            implementation(projects.wasiEmscriptenHost)
            implementation(libs.wsoh.sqlite.st) // TODO: Shouldn't be here, added for resources in tests
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
        }

        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(projects.commonLock)
            }
        }
        androidMain.get().dependsOn(jvmAndAndroidMain)
        jvmMain.get().dependsOn(jvmAndAndroidMain)

        val jvmAndAndroidTest by creating {
            dependsOn(commonTest.get())
            dependencies {
                kotlin(("test-junit"))
                implementation(libs.wsoh.sqlite.mt)
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
                implementation(libs.kotlinx.coroutines.debug)
            }
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)
}

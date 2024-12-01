pluginManagement {
    includeBuild("build-logic/settings")
}

plugins {
    id("ru.pixnews.wasm.sqlite.open.helper.gradle.settings.root")
}

// Workaround for https://github.com/gradle/gradle/issues/26020
buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("androidx.room:androidx.room.gradle.plugin:2.7.0-alpha08")
        classpath("com.android.tools.build:gradle:8.5.2")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:7.0.0.BETA2")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.20-1.0.25")
        classpath("com.saveourtool.diktat:diktat-gradle-plugin:2.0.0")
        classpath("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.7")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0-Beta1")
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.26.1")
    }
}

rootProject.name = "wasm-sqlite-open-helper"

include("common-api")
include("common-lock")
include("common-cleaner")
include("sqlite-common")
include("sqlite-driver")
include("sqlite-embedder-graalvm")
include("sqlite-embedder-chasm")
include("sqlite-embedder-chicory")
include("sqlite-open-helper")
include("sqlite-tests:sqlite-driver-base-tests")
include("sqlite-tests:sqlite-open-helper-base-tests")
include("sqlite-tests:sqlite-test-utils")

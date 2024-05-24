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
        classpath("androidx.room:androidx.room.gradle.plugin:2.7.0-alpha02")
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.0-1.0.21")
        classpath("com.saveourtool.diktat:diktat-gradle-plugin:2.0.0")
        classpath("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.24.0")
    }
}

rootProject.name = "wasm-sqlite-open-helper"

include("common-api")
include("common-lock")
include("common-cleaner")
include("sqlite-common")
include("sqlite-database-path-resolver")
include("sqlite-driver")
include("sqlite-embedder-graalvm")
include("sqlite-embedder-chasm")
include("sqlite-embedder-chicory")
include("sqlite-exception")
include("sqlite-open-helper")
include("native:icu-wasm")
include("native:sqlite-android-wasm-emscripten-icu-mt-pthread-346")
include("native:sqlite-android-wasm-emscripten-icu-346")
include("wasi-emscripten-host")

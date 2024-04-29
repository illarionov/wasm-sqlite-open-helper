pluginManagement {
    includeBuild("build-logic/settings")
}

plugins {
    id("ru.pixnews.wasm-sqlite-open-helper.gradle.settings.root")
}

// Workaround for https://github.com/gradle/gradle/issues/26020
buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("androidx.room:androidx.room.gradle.plugin:2.6.1")
        classpath("com.android.tools.build:gradle:8.2.2")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:6.25.0")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.0.0-RC1-1.0.20")
        classpath("com.saveourtool.diktat:diktat-gradle-plugin:2.0.0")
        classpath("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.6")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0-RC2")
    }
}

rootProject.name = "wasm-sqlite-open-helper"

include("common-api")
include("sqlite-common-api")
include("sqlite-embedder-graalvm")
include("sqlite-embedder-chicory")
include("sqlite-open-helper")
include("native:icu-wasm")
include("native:sqlite-android-wasm-emscripten-icu-mt-pthread-345")
include("native:sqlite-android-wasm-emscripten-icu-345")
include("wasi-emscripten-host")

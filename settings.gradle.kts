pluginManagement {
    includeBuild("build-logic/settings")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
    id("at.released.wasm.sqlite.open.helper.gradle.settings.root")
}

// Workaround for https://github.com/gradle/gradle/issues/26020
buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("androidx.room:androidx.room.gradle.plugin:2.7.0")
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:7.0.3")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.1.21-2.0.1")
        classpath("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
        classpath("at.released.cassettes:cassettes-plugin:0.1-alpha01")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.20")
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.27.0")
    }
}

rootProject.name = "wasm-sqlite-open-helper"

include("common-cleaner")
include("doc:aggregate-documentation")
include("sqlite-common")
include("sqlite-driver")
include("sqlite-embedder-graalvm")
include("sqlite-embedder-chasm")
include("sqlite-embedder-chicory")
include("sqlite-open-helper")
include("sqlite-tests:sqlite-driver-base-tests")
include("sqlite-tests:sqlite-open-helper-base-tests")
include("sqlite-tests:sqlite-test-utils")

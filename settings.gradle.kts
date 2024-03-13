pluginManagement {
    includeBuild("build-logic/settings")
}

plugins {
    id("ru.pixnews.sqlite.open.helper.gradle.settings.root")
}

rootProject.name = "wasm-sqlite-open-helper"

include("common-api")
include("sqlite-common-api")
include("sqlite-open-helper")
include("sqlite-wasm")
include("wasi-emscripten-host")

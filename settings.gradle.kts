pluginManagement {
    includeBuild("build-logic/settings")
}

plugins {
    id("ru.pixnews.sqlite.open.helper.gradle.settings.root")
}

rootProject.name = "wasm-sqlite-open-helper"

include("sqlite-wasm")

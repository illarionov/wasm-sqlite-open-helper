plugins {
    id("at.released.wasm.sqlite.open.helper.gradle.lint.detekt")
    id("at.released.wasm.sqlite.open.helper.gradle.lint.spotless")
}

tasks.register("styleCheck") {
    group = "Verification"
    description = "Runs code style checking tools (excluding tests)"
    dependsOn("detektCheck", "spotlessCheck")
}

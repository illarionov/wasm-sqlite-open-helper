/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

import ru.pixnews.wasm.sqlite.open.helper.gradle.graalvm.GraalvmCompilerJvmArgumentsProvider
import ru.pixnews.wasm.sqlite.open.helper.gradle.graalvm.isRunningOnGraalVm

configurations {
    dependencyScope("graalvmCompiler") {
        defaultDependencies {
            add(versionCatalogs.named("libs").findLibrary("graalvm.compiler").get().get())
        }
    }
    resolvable("graalvmCompilerClasspath") {
        extendsFrom(configurations["graalvmCompiler"])
    }
}

val graalVmJvmArgsProvider = GraalvmCompilerJvmArgumentsProvider(
    configurations["graalvmCompilerClasspath"],
    isRunningOnGraalVm,
)

tasks.withType<JavaExec>().configureEach {
    jvmArgumentProviders += graalVmJvmArgsProvider
}

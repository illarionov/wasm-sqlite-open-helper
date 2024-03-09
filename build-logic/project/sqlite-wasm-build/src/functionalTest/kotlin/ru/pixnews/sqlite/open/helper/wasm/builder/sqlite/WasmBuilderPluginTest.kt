/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.wasm.builder.sqlite

import assertk.assertThat
import assertk.assertions.contains
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class WasmBuilderPluginTest {
    @TempDir
    lateinit var testProjectDir: Path
    private lateinit var settingsFile: Path
    private lateinit var buildFile: Path

    @BeforeEach
    fun setup() {
        settingsFile = testProjectDir.resolve("settings.gradle.kts")
        buildFile = testProjectDir.resolve("build.gradle.kts")
    }

    @Test
    fun `can build project`() {
        settingsFile.writeText("""rootProject.name = "hello-world"""")
        buildFile.writeText(
            """
            plugins {
              id("ru.pixnews.sqlite.open.helper.wasm.builder")
            }

            sqlite3Build {
            }
        """.trimIndent(),
        )

        val runner = GradleRunner.create().apply {
            withPluginClasspath()
            forwardOutput()
            withArguments(
                "--stacktrace",
                "assemble",
            )
            withProjectDir(testProjectDir.toFile())
        }
        val result = runner.build()
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }
}

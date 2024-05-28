/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite

import assertk.assertThat
import assertk.assertions.contains
import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.file.Path
import kotlin.io.path.writeText

class WasmBuilderPluginTest {
    @Rule
    @JvmField
    var testProjectDir = TemporaryFolder()
    private lateinit var settingsFile: Path
    private lateinit var buildFile: Path

    @Before
    fun setup() {
        settingsFile = testProjectDir.root.toPath().resolve("settings.gradle.kts")
        buildFile = testProjectDir.root.toPath().resolve("build.gradle.kts")
    }

    @Test
    fun `can build project`() {
        settingsFile.writeText("""rootProject.name = "hello-world"""")
        buildFile.writeText(
            """
            plugins {
              id("ru.pixnews.wasm.builder.sqlite.plugin")
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
            withProjectDir(testProjectDir.root)
        }
        val result = runner.build()
        assertThat(result.output).contains("BUILD SUCCESSFUL")
    }
}

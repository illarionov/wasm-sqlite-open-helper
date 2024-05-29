/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite

import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class WasmBuilderPluginTest {
    @Test(expected = Test.None::class)
    fun `plugin should apply successfully`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("ru.pixnews.wasm.builder.sqlite.plugin")
        (project as? ProjectInternal)?.evaluate()
    }
}

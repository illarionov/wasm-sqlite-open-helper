/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.gradle.settings.repository

import org.gradle.api.artifacts.dsl.RepositoryHandler
import java.net.URI

internal fun RepositoryHandler.pixnewsMaven() = maven {
    name = "PixnewsMaven"
    url = URI("https://maven.pixnews.ru")
    mavenContent {
        includeGroup("ru.pixnews.wasm-sqlite-open-helper")
        includeGroup("at.released.weh")
    }
}

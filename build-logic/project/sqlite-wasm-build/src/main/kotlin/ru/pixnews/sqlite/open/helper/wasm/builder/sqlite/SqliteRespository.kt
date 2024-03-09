/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.sqlite.open.helper.wasm.builder.sqlite

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import java.net.URI

public fun RepositoryHandler.sqliteRepository(): ArtifactRepository = ivy {
    url = URI("https://www.sqlite.org/")
    patternLayout {
        artifact("2024/sqlite-amalgamation-[revision].[ext]")
    }
    metadataSources {
        artifact()
    }
    content {
        includeModule("sqlite", "amalgamation")
    }
}

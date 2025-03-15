/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.gradle.documentation.dokka

/*
 * Base configuration of dokka
 */
plugins {
    id("org.jetbrains.dokka")
}

@Suppress("UnstableApiUsage")
private val htmlResourcesRoot = layout.settingsDirectory.dir("doc/aggregate-documentation")

dokka {
    dokkaPublications.configureEach {
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(true)
    }

    dokkaSourceSets.configureEach {
        includes.from(
            "MODULE.md",
        )
        sourceLink {
            localDirectory.set(project.layout.projectDirectory)
            val remoteUrlSubpath = project.path.replace(':', '/')
            remoteUrl("https://github.com/illarionov/wasm-sqlite-open-helper/tree/main$remoteUrlSubpath")
        }
    }

    pluginsConfiguration.html {
        homepageLink.set("https://wsoh.released.at")
        footerMessage.set("(C) wasm-sqlite-open-helper project authors and contributors")
        customStyleSheets.from(
            htmlResourcesRoot.file("styles/font-jb-sans-auto.css"),
            htmlResourcesRoot.file("styles/wsoh.css"),
        )
    }
}

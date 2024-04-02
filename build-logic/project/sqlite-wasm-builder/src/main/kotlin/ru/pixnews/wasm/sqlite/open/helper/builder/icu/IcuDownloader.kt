/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.icu

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.file.FileTree
import org.gradle.kotlin.dsl.dependencies

internal fun Project.setupUnpackingIcuAttributes() = dependencies {
    attributesSchema.attribute(EXTRACTED_ICU_ATTRIBUTE)
    artifactTypes.maybeCreate("zip").attributes.apply {
        attribute(EXTRACTED_ICU_ATTRIBUTE, false)
    }
    registerTransform(UnpackIcuTransform::class.java) {
        from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "tgz")
        from.attribute(EXTRACTED_ICU_ATTRIBUTE, false)

        to.attribute(EXTRACTED_ICU_ATTRIBUTE, true)
        to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
    }
}

internal fun Project.createIcuSourceConfiguration(
    icuVersion: String,
): FileTree {
    val icuConfiguration = configurations.detachedConfiguration().attributes {
        attribute(EXTRACTED_ICU_ATTRIBUTE, false)
    }
    icuConfiguration.dependencies.addLater(
        provider {
            dependencyFactory.create("icu", "sources", icuVersion, null, "tgz")
        },
    )

    return icuConfiguration
        .incoming
        .artifactView {
            attributes {
                attribute(EXTRACTED_ICU_ATTRIBUTE, true)
                attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
            }
        }.files.asFileTree
}

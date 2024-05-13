/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

package ru.pixnews.wasm.builder.sqlite.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ZIP_TYPE
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies

internal fun Project.setupUnpackingSqliteAttributes(
    androidSqlitePatchFile: Provider<RegularFile>,
) {
    project.dependencies {
        attributesSchema.attribute(SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE)
        registerTransform(UnpackSqliteAmalgamationTransform::class.java) {
            from.attribute(SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE, SQLITE_ORIGINAL)
            from.attribute(ARTIFACT_TYPE_ATTRIBUTE, ZIP_TYPE)

            to.attribute(SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE, SQLITE_ORIGINAL)
            to.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
        }
        registerTransform(ApplyAndroidPatchesSqliteTransform::class.java) {
            from.attribute(SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE, SQLITE_ORIGINAL)
            from.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)

            to.attribute(SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE, SQLITE_WITH_ANDROID_PATCH)
            to.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)

            parameters {
                this.androidSqlitePatchFile = androidSqlitePatchFile
            }
        }
    }
}

internal fun Project.createSqliteSourceConfiguration(
    sqliteVersion: Provider<String>,
    applyAndroidPatch: Boolean = true,
): FileCollection {
    val sqliteConfiguration = configurations.detachedConfiguration().attributes {
        attribute(SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE, SQLITE_ORIGINAL)
    }
    sqliteConfiguration.dependencies.addLater(
        provider {
            dependencyFactory.create("sqlite", "amalgamation", sqliteVersion.get(), null, "zip")
        },
    )

    val requiredVariant = if (applyAndroidPatch) {
        SQLITE_WITH_ANDROID_PATCH
    } else {
        SQLITE_ORIGINAL
    }

    val unpackedSqliteSrc = sqliteConfiguration
        .incoming
        .artifactView {
            attributes {
                attribute(SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE, requiredVariant)
                attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
            }
        }.files.asFileTree

    val sqlite3c = unpackedSqliteSrc.filter { it.isFile && it.name == "sqlite3.c" }

    return sqlite3c
}

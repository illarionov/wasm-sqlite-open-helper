/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UnstableApiUsage")

package ru.pixnews.wasm.sqlite.open.helper.builder.sqlite.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.DIRECTORY_TYPE
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ZIP_TYPE
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.dependencies

internal val EXTRACTED_SQLITE_ATTRIBUTE: Attribute<Boolean> = Attribute.of(
    "extracted-sqlite",
    Boolean::class.javaObjectType,
)
internal val PATCHED_SQLITE_ATTRIBUTE: Attribute<Boolean> = Attribute.of(
    "patched-sqlite",
    Boolean::class.javaObjectType,
)

internal fun Project.setupUnpackSqliteAttributes(
    androidSqlitePatchFile: Provider<RegularFile>,
) {
    project.dependencies {
        attributesSchema.attribute(EXTRACTED_SQLITE_ATTRIBUTE)
        artifactTypes.maybeCreate("zip").attributes.apply {
            attribute(EXTRACTED_SQLITE_ATTRIBUTE, false)
            attribute(PATCHED_SQLITE_ATTRIBUTE, false)
        }
        registerTransform(UnpackSqliteAmalgamationTransform::class.java) {
            from.attribute(ARTIFACT_TYPE_ATTRIBUTE, ZIP_TYPE)
            from.attribute(EXTRACTED_SQLITE_ATTRIBUTE, false)

            to.attribute(EXTRACTED_SQLITE_ATTRIBUTE, true)
            to.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
        }
        registerTransform(ApplyAndroidPatchesSqliteTransform::class.java) {
            from.attribute(EXTRACTED_SQLITE_ATTRIBUTE, true)
            from.attribute(PATCHED_SQLITE_ATTRIBUTE, false)
            from.attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)

            to.attribute(PATCHED_SQLITE_ATTRIBUTE, true)

            parameters {
                this.androidSqlitePatchFile = androidSqlitePatchFile
            }
        }
    }
}

internal fun Project.createSqliteSourceConfiguration(
    sqliteVersion: String,
    applyAndroidPatch: Boolean = true,
): FileCollection {
    val sqliteConfiguration = configurations.detachedConfiguration(
        dependencyFactory.create("sqlite", "amalgamation", sqliteVersion, null, "zip"),
    ).attributes {
        attribute(EXTRACTED_SQLITE_ATTRIBUTE, false)
        attribute(PATCHED_SQLITE_ATTRIBUTE, false)
    }

    val unpackedSqliteSrc = sqliteConfiguration
        .incoming
        .artifactView {
            attributes {
                attribute(EXTRACTED_SQLITE_ATTRIBUTE, true)
                attribute(PATCHED_SQLITE_ATTRIBUTE, applyAndroidPatch)
                attribute(ARTIFACT_TYPE_ATTRIBUTE, DIRECTORY_TYPE)
            }
        }.files.asFileTree

    val sqlite3c = unpackedSqliteSrc.filter { it.isFile && it.name == "sqlite3.c" }

    return sqlite3c
}

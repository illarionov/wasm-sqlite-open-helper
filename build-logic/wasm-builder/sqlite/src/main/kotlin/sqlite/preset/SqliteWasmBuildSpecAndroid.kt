/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite.preset

import org.gradle.api.Project
import org.gradle.api.file.Directory
import ru.pixnews.wasm.builder.sqlite.SqliteWasmBuildSpec

public fun SqliteWasmBuildSpec.setupAndroidExtensions(
    project: Project,
    sqlite3AndroidSourcesDir: Directory = project.layout.projectDirectory.dir(
        "../sqlite-android-common/android/android",
    ),
) {
    additionalSourceFiles.from(
        sqlite3AndroidSourcesDir.files(
            "sqlite3_android.cpp",
            "PhoneNumberUtils.cpp",
            "OldPhoneNumberUtils.cpp",
        ),
    )
    exportedFunctions.addAll(
        "_register_localized_collators",
        "_register_android_functions",
    )
}

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite.internal

import org.gradle.api.attributes.Attribute

internal val SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE: Attribute<String> = Attribute.of(
    "ru.pixnews.wasm.builder.sqlite.internal.source.sqlite",
    String::class.javaObjectType,
)

/**
 * Original sqlite source code, variant for [SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE]
 */
internal const val SQLITE_ORIGINAL = "sqlite"

/**
 * Sqlite source with Android patch applied, variant for [SQLITE_SOURCE_CODE_VARIANT_ATTRIBUTE]
 */
internal const val SQLITE_WITH_ANDROID_PATCH = "sqlite-patched"

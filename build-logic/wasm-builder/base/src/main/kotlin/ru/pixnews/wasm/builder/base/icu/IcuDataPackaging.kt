/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.base.icu

import org.gradle.api.attributes.Attribute

/**
 * Attribute describes the build and link type of the ICU data
 *
 * https://unicode-org.github.io/icu/userguide/icu_data/#building-and-linking-against-icu-data
 */
public val ICU_DATA_PACKAGING_ATTRIBUTE: Attribute<String> = Attribute.of(
    "ru.pixnews.wasm.builder.base.icu.packaging",
    String::class.javaObjectType,
)

public const val ICU_DATA_PACKAGING_STATIC: String = "static"
public const val ICU_DATA_PACKAGING_FILES: String = "files"
public const val ICU_DATA_PACKAGING_ARCHIVE: String = "archive"

/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.icu.internal

import org.gradle.api.attributes.Attribute

/**
 * Attribute of the uncompressed ICU source code archive
 */
internal val EXTRACTED_ICU_ATTRIBUTE: Attribute<Boolean> = Attribute.of(
    "extracted-icu",
    Boolean::class.javaObjectType,
)

/**
 * An attribute that specifies the ICU toolchain built for the local system, which is then used in cross-compilation
 * to generate and package data.
 *
 * https://unicode-org.github.io/icu/userguide/icu4c/build.html#how-to-cross-compile-icu
 */
internal val ICU_BUILD_TOOLCHAIN_ATTRIBUTE: Attribute<Boolean> = Attribute.of(
    "icu-build-toolchain",
    Boolean::class.javaObjectType,
)

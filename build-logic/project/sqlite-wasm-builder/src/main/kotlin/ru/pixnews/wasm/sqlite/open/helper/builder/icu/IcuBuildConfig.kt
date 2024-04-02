/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.builder.icu

internal const val ICU_STATIC_LIBRARY_RESULT_DIR = "wasmIcu/out"
internal const val ICU_BUILD_TOOLCHAIN_DIR = "wasmIcu/buildA"
internal const val ICU_BUILD_DIR = "wasmIcu/buildB"
internal val ICU_CFLAGS = listOf(
    "-O3",
    "-pthread",
    "-sUSE_PTHREADS",
)
internal val ICU_CXXFLAGS = ICU_CFLAGS
internal val ICU_FORCE_LIBS = listOf(
    "-sUSE_PTHREADS",
    " -pthread",
    "-lm",
)
internal val ICU_PKGDATA_OPTS = listOf(
    "--without-assembly",
    "-O \$(top_builddir)/data/icupkg.inc",
)

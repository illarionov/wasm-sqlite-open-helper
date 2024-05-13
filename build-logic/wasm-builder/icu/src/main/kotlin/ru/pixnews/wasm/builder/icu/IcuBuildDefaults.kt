/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.icu

public object IcuBuildDefaults {
    internal const val ICU_STATIC_LIBRARY_RESULT_DIR: String = "wasmIcu/out"
    internal const val ICU_BUILD_TOOLCHAIN_DIR: String = "wasmIcu/buildA"
    internal const val ICU_BUILD_DIR: String = "wasmIcu/buildB"
    internal const val ICU_DEFAULT_TARGET: String = "wasm32-unknown-emscripten"
    internal const val ICU_USE_PTHREADS: Boolean = true
    internal const val ICU_DATA_DIR: String = ""
    internal val ICU_CFLAGS: List<String> = listOf(
        "-O3",
        "-DU_HAVE_MMAP=0",
        "-DUCONFIG_NO_FILE_IO",
        "-DUCONFIG_NO_FORMATTING",
        "-DUCONFIG_NO_LEGACY_CONVERSION",
        "-DUCONFIG_NO_TRANSLITERATION",
    )
    internal val ICU_PTHREADS_CFLAGS: List<String> = listOf(
        "-pthread",
        "-sUSE_PTHREADS",
    )
    internal val ICU_CXXFLAGS: List<String> = ICU_CFLAGS
    internal val ICU_PTHREADS_CXXFLAGS: List<String> = ICU_PTHREADS_CFLAGS
    internal val ICU_FORCE_LIBS: List<String> = listOf("-lm")
    internal val ICU_PTHREAD_FORCE_LIBS: List<String> = listOf(
        " -pthread",
        "-sUSE_PTHREADS",
    )
    internal val ICU_PKGDATA_OPTS: List<String> = listOf(
        "--without-assembly",
        "-O \$(top_builddir)/data/icupkg.inc",
    )
}

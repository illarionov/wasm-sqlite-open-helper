/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.icu

public enum class IcuBuildFeature(internal val id: String) {
    AUTO_CLEANUP("auto-cleanup"),
    DEBUG("debug"),
    DRAFT("draft"),
    DYLOAD("dyload"),
    EXTRAS("extras"),
    FUZZER("fuzzer"),
    ICUIO("icuio"),
    ICU_CONFIG("icu-config"),
    LAYOUTEX("layoutex"),
    PLUGINS("plugins"),
    RELEASE("release"),
    RENAMING("renaming"),
    SAMPLES("samples"),
    SHARED("shared"),
    STATIC("static"),
    STRICT("strict"),
    TESTS("tests"),
    TOOLS("tools"),
    TRACING("tracing"),
    WEAK_THREADS("weak-threads"),
    ;

    public companion object {
        public val ICU_UPSTREAM_DEFAULT: Set<IcuBuildFeature> = setOf(
            RELEASE,
            RENAMING,
            STRICT,
            SHARED,
            DRAFT,
            DYLOAD,
            EXTRAS,
            ICUIO,
            TOOLS,
            TESTS,
            SAMPLES,
        )
        public val DEFAULT: Set<IcuBuildFeature> = setOf(
            RELEASE,
            RENAMING,
            STATIC,
            STRICT,
            TOOLS,
        )
    }
}

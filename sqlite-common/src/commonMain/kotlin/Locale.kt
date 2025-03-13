/*
 * Copyright 2024-2025, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper

import kotlin.jvm.JvmInline

/**
 * ICU Locale ID
 */
@JvmInline
public value class Locale(
    public val icuId: String,
) {
    public companion object {
        public val EN_US: Locale = Locale("en_US")
    }
}

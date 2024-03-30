/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.ext

import org.graalvm.polyglot.Value
import kotlin.properties.ReadOnlyProperty

internal fun Value.member(): ReadOnlyProperty<Any?, Value> = ReadOnlyProperty { _, property ->
    this@member.getMember(property.name) ?: error("No member ${property.name}")
}

internal fun Boolean.toInt(
    trueValue: Int = 1,
    falseValue: Int = 0,
) = if (this) trueValue else falseValue

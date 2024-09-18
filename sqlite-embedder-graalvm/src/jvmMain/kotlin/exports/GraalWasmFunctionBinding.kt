/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.graalvm.exports

import at.released.weh.host.base.binding.WasmFunctionBinding
import org.graalvm.polyglot.Value

internal class GraalWasmFunctionBinding(
    private val member: Value,
) : WasmFunctionBinding {
    override fun executeVoid(vararg args: Any?) {
        member.execute(*args)
    }

    override fun executeForInt(vararg args: Any?): Int = member.execute(*args).asInt()
    override fun executeForLong(vararg args: Any?): Long = member.execute(*args).asLong()
    override fun executeForFloat(vararg args: Any?): Float = member.execute(*args).asFloat()
    override fun executeForDouble(vararg args: Any?): Double = member.execute(*args).asDouble()
    override fun executeForPtr(vararg args: Any?): Int = member.execute(*args).asInt()
}

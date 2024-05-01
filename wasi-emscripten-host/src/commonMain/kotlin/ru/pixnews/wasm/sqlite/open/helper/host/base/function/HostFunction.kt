/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.function

import ru.pixnews.wasm.sqlite.open.helper.host.WasmValueType

public interface HostFunction {
    public val wasmName: String
    public val type: HostFunctionType

    public interface HostFunctionType {
        public val params: List<WasmValueType>
        public val returnTypes: List<WasmValueType>

        public companion object {
            public operator fun invoke(
                params: List<WasmValueType>,
                returnTypes: List<WasmValueType> = emptyList(),
            ): HostFunctionType = DefaultHostFunctionType(params, returnTypes)

            private data class DefaultHostFunctionType(
                override val params: List<WasmValueType>,
                override val returnTypes: List<WasmValueType>,
            ) : HostFunctionType
        }
    }
}

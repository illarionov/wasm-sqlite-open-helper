/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.builder.sqlite

import org.gradle.api.NamedDomainObjectContainer
import ru.pixnews.wasm.builder.base.WasmBuildDsl
import java.io.Serializable

@WasmBuildDsl
public abstract class SqliteWasmBuilderExtension : Serializable {
    public abstract val builds: NamedDomainObjectContainer<SqliteWasmBuildSpec>

    public companion object {
        private const val serialVersionUID: Long = -1
    }
}

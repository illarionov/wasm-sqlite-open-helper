/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.host.base.function

import ru.pixnews.wasm.sqlite.open.helper.common.api.Logger
import ru.pixnews.wasm.sqlite.open.helper.host.SqliteEmbedderHost

public open class HostFunctionHandle(
    public val function: HostFunction,
    public val host: SqliteEmbedderHost,
) {
    protected val logger: Logger = host.rootLogger.withTag("wasm-func:$${function.wasmName}")
}

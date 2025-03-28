/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.embedder.sqlitecb.function

import at.released.wasm.sqlite.open.helper.InternalWasmSqliteHelperApi
import at.released.weh.common.api.Logger
import at.released.weh.host.EmbedderHost
import at.released.weh.wasm.core.HostFunction

@InternalWasmSqliteHelperApi
public abstract class SqliteHostFunctionHandle(
    public val function: HostFunction,
    public val host: EmbedderHost,
) {
    protected val logger: Logger = host.rootLogger.withTag("SqliteHostFunctionHandle")
}

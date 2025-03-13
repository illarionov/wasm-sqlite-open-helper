/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package at.released.wasm.sqlite.open.helper.internal

import at.released.wasm.sqlite.open.helper.internal.WasmSqliteCleaner.WasmSqliteCleanable

public expect val wasmSqliteCleaner: WasmSqliteCleaner

/**
 * Wrapper over JVM and Android Cleaner. Manages a set of object references and corresponding cleaning actions.
 *
 * See: [java.lang.ref.Cleaner](https://docs.oracle.com/javase%2F9%2Fdocs%2Fapi%2F%2F/java/lang/ref/Cleaner.html)
 *
 */
public interface WasmSqliteCleaner {
    /**
     * Registers an object and a cleaning action to run when the object becomes phantom reachable.
     * Refer to the
     * [API Note](https://docs.oracle.com/javase%2F9%2Fdocs%2Fapi%2F%2F/java/lang/ref/Cleaner.html#compatible-cleaners)
     * for cautions about the behavior of cleaning actions.
     */
    public fun register(obj: Any, cleanAction: () -> Unit): WasmSqliteCleanable

    public fun interface WasmSqliteCleanable {
        public fun clean()
    }
}

internal object NoOpWasmSqliteCleaner : WasmSqliteCleaner {
    override fun register(obj: Any, cleanAction: () -> Unit): WasmSqliteCleanable {
        return WasmSqliteCleanable { cleanAction() }
    }
}

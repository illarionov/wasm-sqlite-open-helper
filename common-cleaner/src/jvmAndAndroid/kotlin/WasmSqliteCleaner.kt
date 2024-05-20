/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import ru.pixnews.wasm.sqlite.open.helper.internal.WasmSqliteCleaner.WasmSqliteCleanable
import java.lang.ref.Cleaner

public actual val wasmSqliteCleaner: WasmSqliteCleaner = run {
    if (isJvmOrAndroidMinApi(33)) {
        @Suppress("NewApi")
        JvmWasmSqliteCleanerImpl()
    } else {
        NoOpWasmSqliteCleaner
    }
}

@Suppress("NewApi", "PrivateApi", "SameParameterValue")
internal fun isJvmOrAndroidMinApi(requireMinApi: Int = 33): Boolean {
    // Use reflection, taking into account that this should work on Android JVM unit tests
    try {
        val sdkIntField = Class.forName("android.os.Build").getDeclaredField("SDK_INT")
        val version = sdkIntField.get(null) as? Int ?: 0
        return version >= requireMinApi
    } catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") ex: Exception) {
        // is JVM
        return true
    }
}

@Suppress("NewApi")
internal class JvmWasmSqliteCleanerImpl : WasmSqliteCleaner {
    private val cleaner = Cleaner.create()
    override fun register(obj: Any, cleanAction: () -> Unit): WasmSqliteCleanable {
        return CleanableAdapter(cleaner.register(obj, cleanAction))
    }

    private class CleanableAdapter(
        private val delegate: Cleaner.Cleanable,
    ) : WasmSqliteCleanable {
        override fun clean() = delegate.clean()
    }
}

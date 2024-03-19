/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.internal

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.TIRAMISU
import androidx.annotation.RequiresApi
import java.lang.ref.Cleaner as JvmCleaner

internal object WasmSqliteCleaner {
    private val cleaner = run {
        val sdkInt = try {
            VERSION.SDK_INT
        } catch (ignore: Exception) {
            0
        }
        if (sdkInt == 0 || VERSION.SDK_INT >= TIRAMISU) {
            @Suppress("NewApi")
            JvmWasmSqliteCleanerImpl()
        } else {
            NoOpWasmSqliteCleanerImpl()
        }
    }

    fun register(obj: Any, runnable: () -> Unit): WasmSqliteCleanable {
        return cleaner.register(obj, runnable)
    }

    public fun interface WasmSqliteCleanable {
        fun clean()
    }

    private interface WasmSqliteCleaner {
        fun register(obj: Any, runnable: () -> Unit): WasmSqliteCleanable
    }

    private class NoOpWasmSqliteCleanerImpl : WasmSqliteCleaner {
        override fun register(obj: Any, runnable: () -> Unit): WasmSqliteCleanable {
            return WasmSqliteCleanable { runnable() }
        }
    }

    @RequiresApi(TIRAMISU)
    private class JvmWasmSqliteCleanerImpl : WasmSqliteCleaner {
        private val cleaner = JvmCleaner.create()
        override fun register(obj: Any, runnable: () -> Unit): WasmSqliteCleanable {
            return CleanableAdapter(cleaner.register(obj, runnable))
        }

        private class CleanableAdapter(
            private val delegate: JvmCleaner.Cleanable,
        ) : WasmSqliteCleanable {
            override fun clean() = delegate.clean()
        }
    }


}

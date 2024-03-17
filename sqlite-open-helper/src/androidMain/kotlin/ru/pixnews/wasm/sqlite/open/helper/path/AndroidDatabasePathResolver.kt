/*
 * Copyright 2024, the wasm-sqlite-open-helper project authors and contributors. Please see the AUTHORS file
 * for details. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 * SPDX-License-Identifier: Apache-2.0
 */

package ru.pixnews.wasm.sqlite.open.helper.path

import android.content.Context
import java.io.File

public class AndroidDatabasePathResolver(
    context: Context,
) : DatabasePathResolver {
    private val context: Context = context.applicationContext
    override fun getDatabasePath(name: String): File = context.getDatabasePath(name)
}
